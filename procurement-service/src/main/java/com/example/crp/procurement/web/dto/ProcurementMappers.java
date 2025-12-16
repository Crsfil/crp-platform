package com.example.crp.procurement.web.dto;

import com.example.crp.procurement.domain.*;

import java.util.List;

public final class ProcurementMappers {
    private ProcurementMappers() {}

    public static ProcurementDtos.RequestSummary toSummary(ProcurementRequest r) {
        return new ProcurementDtos.RequestSummary(
                r.getId(),
                r.getKind(),
                r.getEquipmentId(),
                r.getRequesterId(),
                r.getStatus(),
                r.getAmount(),
                r.getCreatedAt()
        );
    }

    public static ProcurementDtos.RequestDetails toDetails(ProcurementRequest r, List<ProcurementAttachment> attachments) {
        List<ProcurementDtos.RequestLine> lines = r.getLines() == null ? List.of() : r.getLines().stream().map(ProcurementMappers::toLine).toList();
        return new ProcurementDtos.RequestDetails(
                r.getId(),
                r.getKind(),
                r.getTitle(),
                r.getRequestNumber(),
                r.getEquipmentId(),
                r.getRequesterId(),
                r.getStatus(),
                r.getAmount(),
                r.getCurrency(),
                r.getCostCenter(),
                r.getNeedByDate(),
                r.getApprovedAt(),
                r.getApprovedBy(),
                r.getCreatedAt(),
                lines,
                attachments == null ? List.of() : attachments.stream().map(ProcurementMappers::toAttachment).toList()
        );
    }

    public static ProcurementDtos.RequestLine toLine(ProcurementRequestLine l) {
        return new ProcurementDtos.RequestLine(l.getId(), l.getDescription(), l.getQuantity(), l.getUom(), l.getUnitPrice(), l.getNeedByDate());
    }

    public static ProcurementDtos.SupplierDto toSupplier(Supplier s) {
        return new ProcurementDtos.SupplierDto(s.getId(), s.getName(), s.getInn(), s.getKpp(), s.getEmail(), s.getStatus(), s.getCreatedAt());
    }

    public static ProcurementDtos.PurchaseOrderDto toPurchaseOrder(PurchaseOrder po, List<ProcurementAttachment> attachments) {
        List<ProcurementDtos.PurchaseOrderLineDto> lines = po.getLines() == null ? List.of() : po.getLines().stream().map(ProcurementMappers::toPoLine).toList();
        return new ProcurementDtos.PurchaseOrderDto(
                po.getId(),
                po.getRequest() == null ? null : po.getRequest().getId(),
                po.getSupplier() == null ? null : po.getSupplier().getId(),
                po.getStatus(),
                po.getTotalAmount(),
                po.getCreatedAt(),
                lines,
                attachments == null ? List.of() : attachments.stream().map(ProcurementMappers::toAttachment).toList()
        );
    }

    public static ProcurementDtos.PurchaseOrderSummary toPurchaseOrderSummary(PurchaseOrder po) {
        return new ProcurementDtos.PurchaseOrderSummary(
                po.getId(),
                po.getRequest() == null ? null : po.getRequest().getId(),
                po.getSupplier() == null ? null : po.getSupplier().getId(),
                po.getStatus(),
                po.getTotalAmount(),
                po.getCreatedAt()
        );
    }

    public static ProcurementDtos.PurchaseOrderLineDto toPoLine(PurchaseOrderLine l) {
        return new ProcurementDtos.PurchaseOrderLineDto(
                l.getId(),
                l.getRequestLine() == null ? null : l.getRequestLine().getId(),
                l.getDescription(),
                l.getQuantityOrdered(),
                l.getQuantityReceived(),
                l.getUnitPrice()
        );
    }

    public static ProcurementDtos.GoodsReceiptDto toReceipt(GoodsReceipt gr, List<ProcurementAttachment> attachments) {
        List<ProcurementDtos.GoodsReceiptLineDto> lines = gr.getLines() == null ? List.of() : gr.getLines().stream().map(ProcurementMappers::toReceiptLine).toList();
        return new ProcurementDtos.GoodsReceiptDto(
                gr.getId(),
                gr.getPurchaseOrder() == null ? null : gr.getPurchaseOrder().getId(),
                gr.getStatus(),
                gr.getCreatedAt(),
                lines,
                attachments == null ? List.of() : attachments.stream().map(ProcurementMappers::toAttachment).toList()
        );
    }

    public static ProcurementDtos.GoodsReceiptSummary toReceiptSummary(GoodsReceipt gr) {
        return new ProcurementDtos.GoodsReceiptSummary(
                gr.getId(),
                gr.getPurchaseOrder() == null ? null : gr.getPurchaseOrder().getId(),
                gr.getStatus(),
                gr.getCreatedAt()
        );
    }

    public static ProcurementDtos.GoodsReceiptLineDto toReceiptLine(GoodsReceiptLine l) {
        return new ProcurementDtos.GoodsReceiptLineDto(
                l.getId(),
                l.getPurchaseOrderLine() == null ? null : l.getPurchaseOrderLine().getId(),
                l.getQuantityReceived()
        );
    }

    public static ProcurementDtos.Attachment toAttachment(ProcurementAttachment a) {
        return new ProcurementDtos.Attachment(
                a.getId(),
                a.getOwnerType(),
                a.getOwnerId(),
                a.getFileName(),
                a.getContentType(),
                a.getFileSize(),
                a.getSha256(),
                a.getStorageType(),
                a.getStorageLocation(),
                a.getCreatedAt(),
                a.getCreatedBy()
        );
    }
}
