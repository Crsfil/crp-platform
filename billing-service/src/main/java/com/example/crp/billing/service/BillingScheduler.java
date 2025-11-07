package com.example.crp.billing.service;

import com.example.crp.billing.domain.Invoice;
import com.example.crp.billing.repo.InvoiceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class BillingScheduler {
    private final InvoiceRepository invoices;
    private final RestClient scheduleClient;
    private final String internalApiKey;
    private final KafkaTemplate<String,Object> kafka;

    public BillingScheduler(InvoiceRepository invoices,
                            @Value("${schedule.base-url:http://schedule-service:8093}") String scheduleBase,
                            @Value("${security.internal-api-key:}") String internalApiKey,
                            KafkaTemplate<String, Object> kafka) {
        this.invoices = invoices; this.scheduleClient = RestClient.builder().baseUrl(scheduleBase).build(); this.internalApiKey=internalApiKey; this.kafka = kafka;
    }

    @Scheduled(fixedDelayString = "${billing.autorun-ms:60000}")
    public void issueDueInvoices() {
        List<Map> due = scheduleClient.get().uri(uriBuilder -> uriBuilder.path("/schedule/due").queryParam("date", LocalDate.now()).build())
                .header("X-Internal-API-Key", internalApiKey)
                .retrieve().body(List.class);
        if (due == null) return;
        for (Map item : due) {
            Number id = (Number) item.get("id");
            Number agreementId = (Number) item.get("agreementId");
            Number amount = (Number) item.get("amount");
            Invoice inv = new Invoice();
            inv.setAgreementId(agreementId.longValue());
            inv.setAmount(amount.doubleValue());
            inv.setStatus("ISSUED");
            invoices.save(inv);
            // mark schedule item invoiced
            scheduleClient.post().uri("/schedule/"+id.longValue()+"/markInvoiced")
                    .header("X-Internal-API-Key", internalApiKey)
                    .retrieve().toBodilessEntity();
            kafka.send("invoice.issued", Map.of("invoiceId", inv.getId(), "agreementId", inv.getAgreementId(), "amount", inv.getAmount()));
        }
    }
}

