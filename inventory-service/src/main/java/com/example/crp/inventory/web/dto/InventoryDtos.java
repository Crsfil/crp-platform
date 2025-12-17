package com.example.crp.inventory.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public class InventoryDtos {

    public record CreateLocation(
            @NotBlank String code,
            @NotBlank String name,
            Long parentId,
            String type,
            String address,
            String region
    ) {}

    public record LocationDto(
            Long id,
            String code,
            String name,
            String type,
            String status,
            String address,
            String region,
            Long parentId,
            String path,
            Integer level,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record TransferEquipment(
            @NotNull Long toLocationId,
            String responsibleUsername,
            String reason
    ) {}

    public record ChangeStatus(
            @NotBlank String status,
            String reason
    ) {}

    public record MovementDto(
            Long id,
            Long equipmentId,
            Long fromLocationId,
            Long toLocationId,
            String movedBy,
            String reason,
            OffsetDateTime movedAt,
            String correlationId
    ) {}

    public record StatusHistoryDto(
            Long id,
            Long equipmentId,
            String fromStatus,
            String toStatus,
            String changedBy,
            String reason,
            OffsetDateTime changedAt,
            String correlationId
    ) {}

    public record DocumentDto(
            UUID id,
            Long equipmentId,
            String docType,
            String fileName,
            String contentType,
            Long sizeBytes,
            String sha256,
            OffsetDateTime createdAt,
            String createdBy,
            String storageType
    ) {}

    public record StartLease(
            @NotNull Long customerLocationId,
            Long agreementId,
            Long customerId,
            OffsetDateTime expectedReturnAt,
            String note
    ) {}

    public record ReturnLease(
            @NotNull Long returnLocationId,
            String toStatus,
            String note
    ) {}

    public record RepossessLease(
            @NotNull Long repossessLocationId,
            String note
    ) {}

    public record LeaseDto(
            Long id,
            Long equipmentId,
            Long agreementId,
            Long customerId,
            Long issuedFromLocationId,
            Long issuedToLocationId,
            String status,
            OffsetDateTime startAt,
            OffsetDateTime expectedReturnAt,
            OffsetDateTime endAt,
            OffsetDateTime returnedAt,
            OffsetDateTime repossessedAt,
            String note,
            String createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String correlationId
    ) {}

    public record CreateDisposition(
            @NotBlank String type,
            java.math.BigDecimal plannedPrice,
            String currency,
            String counterpartyName,
            String counterpartyInn,
            Long locationId,
            String note
    ) {}

    public record CompleteDisposition(
            java.math.BigDecimal actualPrice,
            OffsetDateTime performedAt
    ) {}

    public record ContractSale(
            String saleMethod,
            String lotNumber,
            String contractNumber
    ) {}

    public record InvoiceSale(
            String invoiceNumber
    ) {}

    public record MarkSalePaid(
            OffsetDateTime paidAt
    ) {}

    public record DispositionDto(
            Long id,
            Long equipmentId,
            String type,
            String status,
            java.math.BigDecimal plannedPrice,
            java.math.BigDecimal actualPrice,
            String currency,
            String counterpartyName,
            String counterpartyInn,
            String saleMethod,
            String lotNumber,
            String contractNumber,
            String invoiceNumber,
            OffsetDateTime paidAt,
            Long locationId,
            OffsetDateTime performedAt,
            String note,
            String createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String correlationId
    ) {}

    public record CreateInspection(
            @NotBlank String type,
            Long locationId,
            String summary
    ) {}

    public record AddInspectionFinding(
            String code,
            @NotBlank String severity,
            String description,
            java.math.BigDecimal estimatedCost
    ) {}

    public record CompleteInspection(
            @NotBlank String conclusion,
            @NotBlank String recommendedAction,
            java.math.BigDecimal estimatedRepairCost
    ) {}

    public record InspectionDto(
            Long id,
            Long equipmentId,
            String type,
            String status,
            Long locationId,
            OffsetDateTime inspectedAt,
            String conclusion,
            String recommendedAction,
            java.math.BigDecimal estimatedRepairCost,
            String summary,
            String createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String correlationId
    ) {}

    public record InspectionFindingDto(
            Long id,
            Long inspectionId,
            String code,
            String severity,
            String description,
            java.math.BigDecimal estimatedCost,
            OffsetDateTime createdAt
    ) {}

    public record InspectionDocumentLinkDto(
            Long id,
            Long inspectionId,
            java.util.UUID documentId,
            String relationType,
            OffsetDateTime createdAt
    ) {}

    public record DispositionDocumentLinkDto(
            Long id,
            Long dispositionId,
            java.util.UUID documentId,
            String relationType,
            OffsetDateTime createdAt
    ) {}

    public record CreateRepairOrder(
            Long inspectionId,
            Long repairLocationId,
            String vendorName,
            String vendorInn,
            java.math.BigDecimal plannedCost,
            String currency,
            String note
    ) {}

    public record StartRepair(
            OffsetDateTime startedAt
    ) {}

    public record CompleteRepair(
            java.math.BigDecimal actualCost,
            OffsetDateTime completedAt,
            Boolean markAvailable,
            Boolean createPostRepairInspection,
            String note
    ) {}

    public record RepairOrderDto(
            Long id,
            Long equipmentId,
            Long inspectionId,
            String status,
            Long repairLocationId,
            String vendorName,
            String vendorInn,
            java.math.BigDecimal plannedCost,
            java.math.BigDecimal actualCost,
            String currency,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            String note,
            String createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String correlationId
    ) {}

    public record RepairDocumentLinkDto(
            Long id,
            Long repairId,
            java.util.UUID documentId,
            String relationType,
            OffsetDateTime createdAt
    ) {}

    public record AddRepairLine(
            @NotBlank String kind,
            @NotBlank String description,
            @NotNull java.math.BigDecimal quantity,
            String uom,
            java.math.BigDecimal unitCost
    ) {}

    public record RepairLineDto(
            Long id,
            Long repairId,
            String kind,
            String description,
            java.math.BigDecimal quantity,
            String uom,
            java.math.BigDecimal unitCost,
            java.math.BigDecimal totalCost,
            OffsetDateTime createdAt
    ) {}

    public record CreateRepossessionCase(
            String triggerReason,
            String decisionRef,
            Long targetLocationId
    ) {}

    public record RepossessionCaseDto(
            Long id,
            Long equipmentId,
            String status,
            String triggerReason,
            String decisionRef,
            Long targetLocationId,
            String initiatedBy,
            OffsetDateTime initiatedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String correlationId
    ) {}

      public record CreateStorageOrder(
              @NotNull Long storageLocationId,
              String vendorName,
              String vendorInn,
              OffsetDateTime slaUntil,
              java.math.BigDecimal expectedCost,
              String currency,
              Long procurementServiceOrderId,
              String note
      ) {}

    public record StorageOrderDto(
            Long id,
            Long equipmentId,
            Long storageLocationId,
            String status,
            String vendorName,
            String vendorInn,
            OffsetDateTime slaUntil,
            OffsetDateTime startedAt,
            OffsetDateTime releasedAt,
            java.math.BigDecimal expectedCost,
            java.math.BigDecimal actualCost,
            String currency,
            Long procurementServiceOrderId,
            String note,
            String createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String correlationId
    ) {}

    public record CreateValuation(
            java.math.BigDecimal valuationAmount,
            java.math.BigDecimal liquidationAmount,
            String currency,
            OffsetDateTime valuatedAt,
            String vendorName,
            String vendorInn,
            String note
    ) {}

    public record ValuationDto(
            Long id,
            Long equipmentId,
            java.math.BigDecimal valuationAmount,
            java.math.BigDecimal liquidationAmount,
            String currency,
            OffsetDateTime valuatedAt,
            String vendorName,
            String vendorInn,
            String note,
            String createdBy,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String correlationId
    ) {}

    public record CustodyDto(
            Long id,
            Long equipmentId,
            Long locationId,
            String custodian,
            OffsetDateTime fromTs,
            OffsetDateTime toTs,
            String reason
    ) {}
}
