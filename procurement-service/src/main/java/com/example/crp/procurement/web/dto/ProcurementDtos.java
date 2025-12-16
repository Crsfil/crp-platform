package com.example.crp.procurement.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ProcurementDtos {

    public record CreateRequest(
            @NotBlank String kind,
            String title,
            String requestNumber,
            Long equipmentId,
            Long requesterId,
            String currency,
            String costCenter,
            LocalDate needByDate,
            @Valid List<CreateRequestLine> lines
    ) {}

    public record CreateRequestLine(
            @NotBlank String description,
            @NotNull BigDecimal quantity,
            @NotBlank String uom,
            BigDecimal unitPrice,
            LocalDate needByDate
    ) {}

    public record RequestSummary(
            Long id,
            String kind,
            Long equipmentId,
            Long requesterId,
            String status,
            BigDecimal amount,
            OffsetDateTime createdAt
    ) {}

    public record RequestDetails(
            Long id,
            String kind,
            String title,
            String requestNumber,
            Long equipmentId,
            Long requesterId,
            String status,
            BigDecimal amount,
            String currency,
            String costCenter,
            LocalDate needByDate,
            OffsetDateTime approvedAt,
            String approvedBy,
            OffsetDateTime createdAt,
            List<RequestLine> lines,
            List<Attachment> attachments
    ) {}

    public record RequestLine(
            Long id,
            String description,
            BigDecimal quantity,
            String uom,
            BigDecimal unitPrice,
            LocalDate needByDate
    ) {}

    public record SupplierDto(
            Long id,
            String name,
            String inn,
            String kpp,
            String email,
            String status,
            OffsetDateTime createdAt
    ) {}

    public record CreateSupplier(
            @NotBlank String name,
            String inn,
            String kpp,
            String email
    ) {}

    public record CreatePurchaseOrder(
            @NotNull Long requestId,
            Long supplierId
    ) {}

    public record PurchaseOrderDto(
            Long id,
            Long requestId,
            Long supplierId,
            String status,
            BigDecimal totalAmount,
            OffsetDateTime createdAt,
            List<PurchaseOrderLineDto> lines,
            List<Attachment> attachments
    ) {}

    public record PurchaseOrderSummary(
            Long id,
            Long requestId,
            Long supplierId,
            String status,
            BigDecimal totalAmount,
            OffsetDateTime createdAt
    ) {}

    public record PurchaseOrderLineDto(
            Long id,
            Long requestLineId,
            String description,
            BigDecimal quantityOrdered,
            BigDecimal quantityReceived,
            BigDecimal unitPrice
    ) {}

    public record CreateGoodsReceipt(
            @NotNull List<CreateGoodsReceiptLine> lines
    ) {}

    public record CreateGoodsReceiptLine(
            @NotNull Long purchaseOrderLineId,
            @NotNull BigDecimal quantityReceived
    ) {}

    public record GoodsReceiptDto(
            Long id,
            Long purchaseOrderId,
            String status,
            OffsetDateTime createdAt,
            List<GoodsReceiptLineDto> lines,
            List<Attachment> attachments
    ) {}

    public record GoodsReceiptSummary(
            Long id,
            Long purchaseOrderId,
            String status,
            OffsetDateTime createdAt
    ) {}

    public record GoodsReceiptLineDto(
            Long id,
            Long purchaseOrderLineId,
            BigDecimal quantityReceived
    ) {}

    public record Attachment(
            UUID id,
            String ownerType,
            Long ownerId,
            String fileName,
            String contentType,
            Long fileSize,
            String sha256,
            String storageType,
            String storageLocation,
            OffsetDateTime createdAt,
            String createdBy
    ) {}
}
