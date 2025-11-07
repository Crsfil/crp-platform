package com.example.crp.accounting.messaging;

import com.example.crp.accounting.domain.LedgerEntry;
import com.example.crp.accounting.repo.LedgerRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class AccountingListeners {
    private final LedgerRepository repo;
    public AccountingListeners(LedgerRepository repo){ this.repo=repo; }

    @KafkaListener(topics = "invoice.issued", groupId = "accounting")
    public void onInvoiceIssued(@Payload Map<String,Object> msg){
        Number invoiceId = (Number) msg.get("invoiceId"); Number amount = (Number) msg.get("amount");
        LedgerEntry e = new LedgerEntry(); e.setType("INVOICE_ISSUED"); e.setRefId(invoiceId==null?null:invoiceId.longValue()); e.setAmount(amount==null?null:amount.doubleValue()); e.setCreatedAt(OffsetDateTime.now()); repo.save(e);
    }

    @KafkaListener(topics = "payment.received", groupId = "accounting")
    public void onPayment(@Payload Map<String,Object> msg){
        Number invoiceId = (Number) msg.get("invoiceId"); Number amount = (Number) msg.get("amount");
        LedgerEntry e = new LedgerEntry(); e.setType("PAYMENT_RECEIVED"); e.setRefId(invoiceId==null?null:invoiceId.longValue()); e.setAmount(amount==null?null:amount.doubleValue()); e.setCreatedAt(OffsetDateTime.now()); repo.save(e);
    }

    @KafkaListener(topics = "invoice.paid", groupId = "accounting")
    public void onInvoicePaid(@Payload Map<String,Object> msg){
        Number invoiceId = (Number) msg.get("invoiceId");
        LedgerEntry e = new LedgerEntry(); e.setType("INVOICE_PAID"); e.setRefId(invoiceId==null?null:invoiceId.longValue()); e.setAmount(null); e.setCreatedAt(OffsetDateTime.now()); repo.save(e);
    }
}

