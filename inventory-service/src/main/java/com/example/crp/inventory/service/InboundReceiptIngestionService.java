package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.InboundReceipt;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.InboundReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InboundReceiptIngestionService {

    private static final String STATUS_AVAILABLE = "AVAILABLE";

    private final InboundReceiptRepository inboundReceiptRepository;
    private final EquipmentRepository equipmentRepository;
    private final OutboxService outboxService;

    public InboundReceiptIngestionService(InboundReceiptRepository inboundReceiptRepository,
                                         EquipmentRepository equipmentRepository,
                                         OutboxService outboxService) {
        this.inboundReceiptRepository = inboundReceiptRepository;
        this.equipmentRepository = equipmentRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public void ingest(Events.GoodsReceiptAccepted msg) {
        if (msg == null || msg.receiptId() == null) {
            return;
        }
        if (inboundReceiptRepository.existsById(msg.receiptId())) {
            return;
        }

        InboundReceipt ir = new InboundReceipt();
        ir.setReceiptId(msg.receiptId());
        ir.setPurchaseOrderId(msg.purchaseOrderId());
        ir.setRequestId(msg.requestId());
        ir.setSupplierId(msg.supplierId());
        inboundReceiptRepository.save(ir);

        int createdCount = 0;
        List<Events.GoodsReceiptItem> items = msg.items() == null ? List.of() : msg.items();
        for (Events.GoodsReceiptItem item : items) {
            if (item == null || item.quantityReceived() == null) {
                continue;
            }
            int count = toUnitCount(item.quantityReceived());
            if (count <= 0) {
                continue;
            }
            if (count > 1000) {
                throw new IllegalArgumentException("Too many units in a single receipt line: " + count);
            }
            for (int i = 0; i < count; i++) {
                Equipment e = new Equipment();
                e.setType(trim(item.description(), 128));
                e.setModel(null);
                e.setStatus(STATUS_AVAILABLE);
                e.setPrice(item.unitPrice());
                e.setSourceReceiptId(msg.receiptId());
                e.setSourcePurchaseOrderId(msg.purchaseOrderId());
                e.setSourceRequestId(msg.requestId());
                e.setSourceSupplierId(msg.supplierId());
                e.setSourcePurchaseOrderLineId(item.purchaseOrderLineId());
                e.setSourceRequestLineId(item.requestLineId());
                Equipment saved = equipmentRepository.save(e);
                if (saved.getInventoryNumber() == null || saved.getInventoryNumber().isBlank()) {
                    saved.setInventoryNumber("INV-" + saved.getId());
                    equipmentRepository.save(saved);
                }
                createdCount++;
            }
        }

        outboxService.enqueue("InboundReceipt", msg.receiptId(), "inventory.inbound_receipt.processed",
                "InventoryInboundReceiptProcessed", new Events.InventoryInboundReceiptProcessed(
                        msg.receiptId(),
                        msg.purchaseOrderId(),
                        msg.requestId(),
                        msg.supplierId(),
                        createdCount
                ));
    }

    private static int toUnitCount(BigDecimal qty) {
        try {
            return qty.intValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Non-integer quantityReceived is not supported for equipment: " + qty);
        }
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max);
    }
}
