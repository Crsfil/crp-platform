package com.example.crp.billing.messaging;

import com.example.crp.billing.domain.Invoice;
import com.example.crp.billing.repo.InvoiceRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PaymentListener {
    private final InvoiceRepository invoices;
    private final InvalidMessageRouter invalidRouter;
    public PaymentListener(InvoiceRepository invoices, InvalidMessageRouter invalidRouter) { this.invoices = invoices; this.invalidRouter = invalidRouter; }

    @KafkaListener(topics = "payment.received", groupId = "billing")
    public void onPayment(List<ConsumerRecord<String, Map<String,Object>>> records) {
        for (ConsumerRecord<String, Map<String, Object>> rec : records) {
            String key = rec.key();
            Map<String,Object> payload = rec.value();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("payment.received.invalid", key, payload, "key previously invalid");
                continue;
            }
            Object invId = payload != null ? payload.get("invoiceId") : null;
            if (invId == null) {
                invalidRouter.routeInvalid("payment.received.invalid", key, payload, "missing invoiceId");
                continue;
            }
            Long id = Long.valueOf(String.valueOf(invId));
            try {
                invoices.findById(id).ifPresentOrElse(inv -> {
                    inv.setStatus("PAID");
                    invoices.save(inv);
                }, () -> invalidRouter.routeInvalid("payment.received.invalid", key, payload, "invoice not found"));
            } catch (Exception ex) {
                invalidRouter.routeInvalid("payment.received.invalid", key, payload, ex.getMessage());
            }
        }
    }
}
