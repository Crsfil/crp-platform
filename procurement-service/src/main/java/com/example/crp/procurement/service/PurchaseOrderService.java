package com.example.crp.procurement.service;

import com.example.crp.procurement.domain.*;
import com.example.crp.procurement.messaging.Events;
import com.example.crp.procurement.outbox.OutboxService;
import com.example.crp.procurement.repo.GoodsReceiptRepository;
import com.example.crp.procurement.repo.ProcurementRequestRepository;
import com.example.crp.procurement.repo.PurchaseOrderRepository;
import com.example.crp.procurement.repo.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseOrderService {
    private final ProcurementRequestRepository requestRepository;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final OutboxService outboxService;

    public PurchaseOrderService(ProcurementRequestRepository requestRepository,
                                SupplierRepository supplierRepository,
                                PurchaseOrderRepository purchaseOrderRepository,
                                GoodsReceiptRepository goodsReceiptRepository,
                                OutboxService outboxService) {
        this.requestRepository = requestRepository;
        this.supplierRepository = supplierRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public PurchaseOrder createFromRequest(Long requestId, Long supplierId) {
        ProcurementRequest pr = requestRepository.findWithLinesById(requestId).orElseThrow();
        if (!"APPROVED".equals(pr.getStatus())) {
            throw new IllegalStateException("Request must be APPROVED");
        }
        if (!"PURCHASE".equalsIgnoreCase(pr.getKind())) {
            throw new IllegalStateException("Request kind must be PURCHASE");
        }
        Supplier supplier = null;
        if (supplierId != null) {
            supplier = supplierRepository.findById(supplierId).orElseThrow();
            if (!"ACTIVE".equalsIgnoreCase(supplier.getStatus())) {
                throw new IllegalStateException("Supplier must be ACTIVE");
            }
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setRequest(pr);
        po.setSupplier(supplier);
        po.setStatus("DRAFT");

        BigDecimal total = BigDecimal.ZERO;
        if (pr.getLines() != null) {
            for (ProcurementRequestLine prl : pr.getLines()) {
                PurchaseOrderLine pol = new PurchaseOrderLine();
                pol.setPurchaseOrder(po);
                pol.setRequestLine(prl);
                pol.setDescription(prl.getDescription());
                pol.setQuantityOrdered(prl.getQuantity());
                pol.setUnitPrice(prl.getUnitPrice());
                po.getLines().add(pol);
                if (prl.getUnitPrice() != null && prl.getQuantity() != null) {
                    total = total.add(prl.getUnitPrice().multiply(prl.getQuantity()));
                }
            }
        }
        po.setTotalAmount(total);

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        pr.setStatus("PO_CREATED");
        requestRepository.save(pr);

        outboxService.enqueue("PurchaseOrder", saved.getId(), "procurement.po_created",
                "PurchaseOrderCreated", new Events.PurchaseOrderCreated(saved.getId(), pr.getId(), supplier == null ? null : supplier.getId()));
        return saved;
    }

    @Transactional
    public PurchaseOrder send(Long poId) {
        PurchaseOrder po = purchaseOrderRepository.findWithLinesById(poId).orElseThrow();
        if (!"DRAFT".equals(po.getStatus())) {
            throw new IllegalStateException("PO must be DRAFT");
        }
        po.setStatus("SENT");
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        outboxService.enqueue("PurchaseOrder", saved.getId(), "procurement.po_sent",
                "PurchaseOrderSent", new Events.PurchaseOrderSent(saved.getId()));
        return saved;
    }

    @Transactional
    public GoodsReceipt receive(Long poId, List<ReceiptLineIn> lines) {
        PurchaseOrder po = purchaseOrderRepository.findWithLinesById(poId).orElseThrow();
        if (!"SENT".equals(po.getStatus()) && !"PARTIALLY_RECEIVED".equals(po.getStatus())) {
            throw new IllegalStateException("PO must be SENT or PARTIALLY_RECEIVED");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Receipt must contain lines");
        }

        Map<Long, PurchaseOrderLine> byId = new HashMap<>();
        for (PurchaseOrderLine pol : po.getLines()) {
            byId.put(pol.getId(), pol);
        }

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setPurchaseOrder(po);
        receipt.setStatus("RECEIVED");
        for (ReceiptLineIn in : lines) {
            PurchaseOrderLine pol = byId.get(in.purchaseOrderLineId());
            if (pol == null) {
                throw new IllegalArgumentException("Unknown purchaseOrderLineId=" + in.purchaseOrderLineId());
            }
            if (in.quantityReceived() == null || in.quantityReceived().signum() <= 0) {
                throw new IllegalArgumentException("quantityReceived must be > 0");
            }
            BigDecimal newReceived = pol.getQuantityReceived().add(in.quantityReceived());
            if (pol.getQuantityOrdered() != null && newReceived.compareTo(pol.getQuantityOrdered()) > 0) {
                throw new IllegalStateException("Received quantity exceeds ordered for line " + pol.getId());
            }
            pol.setQuantityReceived(newReceived);

            GoodsReceiptLine grl = new GoodsReceiptLine();
            grl.setReceipt(receipt);
            grl.setPurchaseOrderLine(pol);
            grl.setQuantityReceived(in.quantityReceived());
            receipt.getLines().add(grl);
        }

        boolean fullyReceived = po.getLines().stream().allMatch(l ->
                l.getQuantityOrdered() == null || l.getQuantityReceived().compareTo(l.getQuantityOrdered()) >= 0
        );
        po.setStatus(fullyReceived ? "RECEIVED" : "PARTIALLY_RECEIVED");
        purchaseOrderRepository.save(po);

        GoodsReceipt savedReceipt = goodsReceiptRepository.save(receipt);
        outboxService.enqueue("GoodsReceipt", savedReceipt.getId(), "procurement.goods_received",
                "GoodsReceiptCreated", new Events.GoodsReceiptCreated(savedReceipt.getId(), po.getId()));

        return savedReceipt;
    }

    @Transactional
    public GoodsReceipt acceptReceipt(Long receiptId) {
        GoodsReceipt gr = goodsReceiptRepository.findWithLinesById(receiptId).orElse(null);
        if (gr == null) {
            throw new java.util.NoSuchElementException("Receipt not found");
        }
        if (!"RECEIVED".equals(gr.getStatus())) {
            throw new IllegalStateException("Receipt must be RECEIVED");
        }
        gr.setStatus("ACCEPTED");
        GoodsReceipt saved = goodsReceiptRepository.save(gr);

        PurchaseOrder po = saved.getPurchaseOrder();
        Long requestId = po.getRequest() == null ? null : po.getRequest().getId();
        Long supplierId = po.getSupplier() == null ? null : po.getSupplier().getId();
        java.util.List<Events.GoodsReceiptItem> items = saved.getLines() == null ? java.util.List.of() : saved.getLines().stream().map(l -> {
            PurchaseOrderLine pol = l.getPurchaseOrderLine();
            ProcurementRequestLine prl = pol == null ? null : pol.getRequestLine();
            String uom = prl == null ? null : prl.getUom();
            Long prlId = prl == null ? null : prl.getId();
            String desc = pol == null ? null : pol.getDescription();
            java.math.BigDecimal unitPrice = pol == null ? null : pol.getUnitPrice();
            return new Events.GoodsReceiptItem(
                    pol == null ? null : pol.getId(),
                    prlId,
                    desc,
                    l.getQuantityReceived(),
                    uom,
                    unitPrice
            );
        }).toList();

        outboxService.enqueue("GoodsReceipt", saved.getId(), "procurement.goods_accepted",
                "GoodsReceiptAccepted", new Events.GoodsReceiptAccepted(saved.getId(), po.getId(), requestId, supplierId, items));
        return saved;
    }

    @Transactional
    public GoodsReceipt rejectReceipt(Long receiptId) {
        GoodsReceipt gr = goodsReceiptRepository.findById(receiptId).orElseThrow();
        if (!"RECEIVED".equals(gr.getStatus())) {
            throw new IllegalStateException("Receipt must be RECEIVED");
        }
        gr.setStatus("REJECTED");
        return goodsReceiptRepository.save(gr);
    }

    public record ReceiptLineIn(Long purchaseOrderLineId, BigDecimal quantityReceived) {}
}
