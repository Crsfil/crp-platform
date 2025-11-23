package com.example.crp.accounting.messaging;

import com.example.crp.accounting.domain.LedgerEntry;
import com.example.crp.accounting.repo.LedgerRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Component
public class AccountingListeners {
    private final LedgerRepository repo;
    private final InvalidMessageRouter invalidRouter;
    public AccountingListeners(LedgerRepository repo, InvalidMessageRouter invalidRouter){ this.repo=repo; this.invalidRouter = invalidRouter; }

    @KafkaListener(topics = "invoice.issued", groupId = "accounting")
    public void onInvoiceIssued(List<ConsumerRecord<String, Map<String,Object>>> records){
        for (ConsumerRecord<String, Map<String, Object>> rec : records) {
            String key = rec.key();
            Map<String,Object> msg = rec.value();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("invoice.issued.invalid", key, msg, "key previously invalid");
                continue;
            }
            Number invoiceId = msg != null ? (Number) msg.get("invoiceId") : null;
            Number amount = msg != null ? (Number) msg.get("amount") : null;
            if (invoiceId == null) {
                invalidRouter.routeInvalid("invoice.issued.invalid", key, msg, "missing invoiceId");
                continue;
            }
            try {
                LedgerEntry e = new LedgerEntry(); e.setType("INVOICE_ISSUED"); e.setRefId(invoiceId.longValue()); e.setAmount(amount==null?null:amount.doubleValue()); e.setCreatedAt(OffsetDateTime.now()); repo.save(e);
            } catch (Exception ex) {
                invalidRouter.routeInvalid("invoice.issued.invalid", key, msg, ex.getMessage());
            }
        }
    }

    @KafkaListener(topics = "payment.received", groupId = "accounting")
    public void onPayment(List<ConsumerRecord<String, Map<String,Object>>> records){
        for (ConsumerRecord<String, Map<String, Object>> rec : records) {
            String key = rec.key();
            Map<String,Object> msg = rec.value();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("payment.received.invalid", key, msg, "key previously invalid");
                continue;
            }
            Number invoiceId = msg != null ? (Number) msg.get("invoiceId") : null;
            Number amount = msg != null ? (Number) msg.get("amount") : null;
            if (invoiceId == null) {
                invalidRouter.routeInvalid("payment.received.invalid", key, msg, "missing invoiceId");
                continue;
            }
            try {
                LedgerEntry e = new LedgerEntry(); e.setType("PAYMENT_RECEIVED"); e.setRefId(invoiceId.longValue()); e.setAmount(amount==null?null:amount.doubleValue()); e.setCreatedAt(OffsetDateTime.now()); repo.save(e);
            } catch (Exception ex) {
                invalidRouter.routeInvalid("payment.received.invalid", key, msg, ex.getMessage());
            }
        }
    }

    @KafkaListener(topics = "invoice.paid", groupId = "accounting")
    public void onInvoicePaid(List<ConsumerRecord<String, Map<String,Object>>> records){
        for (ConsumerRecord<String, Map<String, Object>> rec : records) {
            String key = rec.key();
            Map<String,Object> msg = rec.value();
            if (invalidRouter.isMarkedInvalid(key)) {
                invalidRouter.routeInvalid("invoice.paid.invalid", key, msg, "key previously invalid");
                continue;
            }
            Number invoiceId = msg != null ? (Number) msg.get("invoiceId") : null;
            if (invoiceId == null) {
                invalidRouter.routeInvalid("invoice.paid.invalid", key, msg, "missing invoiceId");
                continue;
            }
            try {
                LedgerEntry e = new LedgerEntry(); e.setType("INVOICE_PAID"); e.setRefId(invoiceId.longValue()); e.setAmount(null); e.setCreatedAt(OffsetDateTime.now()); repo.save(e);
            } catch (Exception ex) {
                invalidRouter.routeInvalid("invoice.paid.invalid", key, msg, ex.getMessage());
            }
        }
    }
}
