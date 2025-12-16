package com.example.crp.procurement.messaging;

import java.math.BigDecimal;
import java.util.List;

public class Events {
    public record ProcurementRequested(Long requestId, Long equipmentId, Long requesterId) {}
    public record ProcurementApproved(Long requestId, Long equipmentId, Long approverId) {}
    public record ProcurementRejected(Long requestId, Long equipmentId, Long approverId) {}
    public record InventoryReserved(Long requestId, Long equipmentId) {}
    public record InventoryReserveFailed(Long requestId, Long equipmentId, String reason) {}
    public record InventoryReleased(Long requestId, Long equipmentId) {}

    public record PurchaseOrderCreated(Long purchaseOrderId, Long requestId, Long supplierId) {}
    public record PurchaseOrderSent(Long purchaseOrderId) {}
    public record GoodsReceiptCreated(Long receiptId, Long purchaseOrderId) {}
    public record GoodsReceiptAccepted(Long receiptId,
                                       Long purchaseOrderId,
                                       Long requestId,
                                       Long supplierId,
                                       List<GoodsReceiptItem> items) {}

    public record GoodsReceiptItem(Long purchaseOrderLineId,
                                   Long requestLineId,
                                   String description,
                                   BigDecimal quantityReceived,
                                   String uom,
                                   BigDecimal unitPrice) {}
}
