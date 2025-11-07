package com.example.crp.billing.messaging;

import com.example.crp.billing.domain.Invoice;
import com.example.crp.billing.repo.InvoiceRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentListener {
    private final InvoiceRepository invoices;
    public PaymentListener(InvoiceRepository invoices) { this.invoices = invoices; }

    @KafkaListener(topics = "payment.received", groupId = "billing")
    public void onPayment(@Payload Map<String,Object> payload) {
        Object invId = payload.get("invoiceId");
        if (invId == null) return;
        Long id = Long.valueOf(String.valueOf(invId));
        invoices.findById(id).ifPresent(inv -> { inv.setStatus("PAID"); invoices.save(inv); });
    }
}

