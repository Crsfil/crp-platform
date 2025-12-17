package com.example.crp.inventory.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "equipment_storage_order")
public class EquipmentStorageOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "storage_location_id", nullable = false)
    private Long storageLocationId;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // CREATED, IN_PROGRESS, COMPLETED, RELEASED, CANCELED

    @Column(name = "vendor_name", length = 256)
    private String vendorName;

    @Column(name = "vendor_inn", length = 32)
    private String vendorInn;

    @Column(name = "sla_until")
    private OffsetDateTime slaUntil;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "expected_cost", precision = 18, scale = 2)
    private BigDecimal expectedCost;

    @Column(name = "actual_cost", precision = 18, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "procurement_service_order_id")
    private Long procurementServiceOrderId;

    @Column(name = "note", length = 512)
    private String note;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    public Long getId() { return id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getStorageLocationId() { return storageLocationId; }
    public void setStorageLocationId(Long storageLocationId) { this.storageLocationId = storageLocationId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public String getVendorInn() { return vendorInn; }
    public void setVendorInn(String vendorInn) { this.vendorInn = vendorInn; }
    public OffsetDateTime getSlaUntil() { return slaUntil; }
    public void setSlaUntil(OffsetDateTime slaUntil) { this.slaUntil = slaUntil; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getReleasedAt() { return releasedAt; }
    public void setReleasedAt(OffsetDateTime releasedAt) { this.releasedAt = releasedAt; }
    public BigDecimal getExpectedCost() { return expectedCost; }
    public void setExpectedCost(BigDecimal expectedCost) { this.expectedCost = expectedCost; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Long getProcurementServiceOrderId() { return procurementServiceOrderId; }
    public void setProcurementServiceOrderId(Long procurementServiceOrderId) { this.procurementServiceOrderId = procurementServiceOrderId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (startedAt == null) {
            startedAt = createdAt;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (status == null || status.isBlank()) {
            status = "IN_PROGRESS";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
