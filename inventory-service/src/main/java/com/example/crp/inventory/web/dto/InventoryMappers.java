package com.example.crp.inventory.web.dto;

import com.example.crp.inventory.domain.EquipmentDisposition;
import com.example.crp.inventory.domain.EquipmentDispositionDocumentLink;
import com.example.crp.inventory.domain.EquipmentDocument;
import com.example.crp.inventory.domain.EquipmentLease;
import com.example.crp.inventory.domain.EquipmentMovement;
import com.example.crp.inventory.domain.EquipmentInspection;
import com.example.crp.inventory.domain.EquipmentInspectionFinding;
import com.example.crp.inventory.domain.EquipmentInspectionDocumentLink;
import com.example.crp.inventory.domain.EquipmentRepairOrder;
import com.example.crp.inventory.domain.EquipmentRepairDocumentLink;
import com.example.crp.inventory.domain.EquipmentRepairLine;
import com.example.crp.inventory.domain.EquipmentStatusHistory;
import com.example.crp.inventory.domain.Location;

public class InventoryMappers {

    public static InventoryDtos.LocationDto toLocation(Location l) {
        return new InventoryDtos.LocationDto(
                l.getId(),
                l.getCode(),
                l.getName(),
                l.getType(),
                l.getStatus(),
                l.getAddress(),
                l.getRegion(),
                l.getParentId(),
                l.getPath(),
                l.getLevel(),
                l.getCreatedAt(),
                l.getUpdatedAt()
        );
    }

    public static InventoryDtos.MovementDto toMovement(EquipmentMovement m) {
        return new InventoryDtos.MovementDto(
                m.getId(),
                m.getEquipmentId(),
                m.getFromLocationId(),
                m.getToLocationId(),
                m.getMovedBy(),
                m.getReason(),
                m.getMovedAt(),
                m.getCorrelationId()
        );
    }

    public static InventoryDtos.StatusHistoryDto toStatusHistory(EquipmentStatusHistory h) {
        return new InventoryDtos.StatusHistoryDto(
                h.getId(),
                h.getEquipmentId(),
                h.getFromStatus(),
                h.getToStatus(),
                h.getChangedBy(),
                h.getReason(),
                h.getChangedAt(),
                h.getCorrelationId()
        );
    }

    public static InventoryDtos.DocumentDto toDocument(EquipmentDocument d) {
        return new InventoryDtos.DocumentDto(
                d.getId(),
                d.getEquipmentId(),
                d.getDocType(),
                d.getFileName(),
                d.getContentType(),
                d.getSizeBytes(),
                d.getSha256(),
                d.getCreatedAt(),
                d.getCreatedBy(),
                d.getStorageType()
        );
    }

    public static InventoryDtos.LeaseDto toLease(EquipmentLease l) {
        return new InventoryDtos.LeaseDto(
                l.getId(),
                l.getEquipmentId(),
                l.getAgreementId(),
                l.getCustomerId(),
                l.getIssuedFromLocationId(),
                l.getIssuedToLocationId(),
                l.getStatus(),
                l.getStartAt(),
                l.getExpectedReturnAt(),
                l.getEndAt(),
                l.getReturnedAt(),
                l.getRepossessedAt(),
                l.getNote(),
                l.getCreatedBy(),
                l.getCreatedAt(),
                l.getUpdatedAt(),
                l.getCorrelationId()
        );
    }

    public static InventoryDtos.DispositionDto toDisposition(EquipmentDisposition d) {
        return new InventoryDtos.DispositionDto(
                d.getId(),
                d.getEquipmentId(),
                d.getType(),
                d.getStatus(),
                d.getPlannedPrice(),
                d.getActualPrice(),
                d.getCurrency(),
                d.getCounterpartyName(),
                d.getCounterpartyInn(),
                d.getSaleMethod(),
                d.getLotNumber(),
                d.getContractNumber(),
                d.getInvoiceNumber(),
                d.getPaidAt(),
                d.getLocationId(),
                d.getPerformedAt(),
                d.getNote(),
                d.getCreatedBy(),
                d.getCreatedAt(),
                d.getUpdatedAt(),
                d.getCorrelationId()
        );
    }

    public static InventoryDtos.InspectionDto toInspection(EquipmentInspection i) {
        return new InventoryDtos.InspectionDto(
                i.getId(),
                i.getEquipmentId(),
                i.getType(),
                i.getStatus(),
                i.getLocationId(),
                i.getInspectedAt(),
                i.getConclusion(),
                i.getRecommendedAction(),
                i.getEstimatedRepairCost(),
                i.getSummary(),
                i.getCreatedBy(),
                i.getCreatedAt(),
                i.getUpdatedAt(),
                i.getCorrelationId()
        );
    }

    public static InventoryDtos.InspectionFindingDto toInspectionFinding(EquipmentInspectionFinding f) {
        return new InventoryDtos.InspectionFindingDto(
                f.getId(),
                f.getInspectionId(),
                f.getCode(),
                f.getSeverity(),
                f.getDescription(),
                f.getEstimatedCost(),
                f.getCreatedAt()
        );
    }

    public static InventoryDtos.InspectionDocumentLinkDto toInspectionDocument(EquipmentInspectionDocumentLink d) {
        return new InventoryDtos.InspectionDocumentLinkDto(
                d.getId(),
                d.getInspectionId(),
                d.getDocumentId(),
                d.getRelationType(),
                d.getCreatedAt()
        );
    }

    public static InventoryDtos.DispositionDocumentLinkDto toDispositionDocument(EquipmentDispositionDocumentLink d) {
        return new InventoryDtos.DispositionDocumentLinkDto(
                d.getId(),
                d.getDispositionId(),
                d.getDocumentId(),
                d.getRelationType(),
                d.getCreatedAt()
        );
    }

    public static InventoryDtos.RepairOrderDto toRepair(EquipmentRepairOrder r) {
        return new InventoryDtos.RepairOrderDto(
                r.getId(),
                r.getEquipmentId(),
                r.getInspectionId(),
                r.getStatus(),
                r.getRepairLocationId(),
                r.getVendorName(),
                r.getVendorInn(),
                r.getPlannedCost(),
                r.getActualCost(),
                r.getCurrency(),
                r.getStartedAt(),
                r.getCompletedAt(),
                r.getNote(),
                r.getCreatedBy(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getCorrelationId()
        );
    }

    public static InventoryDtos.RepairDocumentLinkDto toRepairDocument(EquipmentRepairDocumentLink d) {
        return new InventoryDtos.RepairDocumentLinkDto(
                d.getId(),
                d.getRepairId(),
                d.getDocumentId(),
                d.getRelationType(),
                d.getCreatedAt()
        );
    }

    public static InventoryDtos.RepairLineDto toRepairLine(EquipmentRepairLine l) {
        return new InventoryDtos.RepairLineDto(
                l.getId(),
                l.getRepairId(),
                l.getKind(),
                l.getDescription(),
                l.getQuantity(),
                l.getUom(),
                l.getUnitCost(),
                l.getTotalCost(),
                l.getCreatedAt()
        );
    }
}
