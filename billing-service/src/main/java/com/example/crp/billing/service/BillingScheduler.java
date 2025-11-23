package com.example.crp.billing.service;

import com.example.crp.billing.domain.Invoice;
import com.example.crp.billing.outbox.OutboxService;
import com.example.crp.billing.repo.InvoiceRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class BillingScheduler {
    private final InvoiceRepository invoices;
    private final WebClient scheduleClient;
    private final OutboxService outboxService;

    public BillingScheduler(InvoiceRepository invoices,
                            WebClient scheduleClient,
                            OutboxService outboxService) {
        this.invoices = invoices; this.scheduleClient = scheduleClient; this.outboxService = outboxService;
    }

    @Scheduled(fixedDelayString = "${billing.autorun-ms:60000}")
    @CircuitBreaker(name = "scheduleClient", fallbackMethod = "onScheduleFailure")
    @Retry(name = "scheduleClient")
    public void issueDueInvoices() {
        List<Map> due = scheduleClient.get()
                .uri(uriBuilder -> uriBuilder.path("/schedule/due").queryParam("date", LocalDate.now()).build())
                .retrieve()
                .bodyToMono(List.class)
                .block();
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
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            outboxService.enqueue("Invoice", inv.getId(), "invoice.issued", "InvoiceIssued",
                    Map.of("invoiceId", inv.getId(), "agreementId", inv.getAgreementId(), "amount", inv.getAmount()));
        }
    }

    // fallback signature for circuit breaker
    private void onScheduleFailure(Throwable t) {
        // No-op fallback; failures are visible via tracing/metrics and will be retried via @Retry
    }
}
