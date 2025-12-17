package com.example.crp.procurement.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_service_order")
public class ProcurementServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_type", nullable = false, length = 64)
    private String serviceType;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "vendor_name", length = 256)
    private String vendorName;

    @Column(name = "vendor_inn", length = 32)
    private String vendorInn;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // CREATED, IN_PROGRESS, COMPLETED, CANCELED

    @Column(name = "sla_until")
    private OffsetDateTime slaUntil;

    @Column(name = "planned_cost", precision = 18, scale = 2)
    private BigDecimal plannedCost;

    @Column(name = "actual_cost", precision = 18, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "act_document_id")
    private UUID actDocumentId;

    @Column(name = "note", length = 512)
    private String note;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public String getVendorInn() { return vendorInn; }
    public void setVendorInn(String vendorInn) { this.vendorInn = vendorInn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getSlaUntil() { return slaUntil; }
    public void setSlaUntil(OffsetDateTime slaUntil) { this.slaUntil = slaUntil; }
    public BigDecimal getPlannedCost() { return plannedCost; }
    public void setPlannedCost(BigDecimal plannedCost) { this.plannedCost = plannedCost; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public UUID getActDocumentId() { return actDocumentId; }
    public void setActDocumentId(UUID actDocumentId) { this.actDocumentId = actDocumentId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
        if (status == null || status.isBlank()) status = "CREATED";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
