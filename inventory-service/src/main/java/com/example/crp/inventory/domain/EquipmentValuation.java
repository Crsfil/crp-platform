package com.example.crp.inventory.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "equipment_valuation")
public class EquipmentValuation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "valuation_amount", precision = 18, scale = 2)
    private BigDecimal valuationAmount;

    @Column(name = "liquidation_amount", precision = 18, scale = 2)
    private BigDecimal liquidationAmount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "valuated_at")
    private OffsetDateTime valuatedAt;

    @Column(name = "vendor_name", length = 256)
    private String vendorName;

    @Column(name = "vendor_inn", length = 32)
    private String vendorInn;

    @Column(name = "report_document_id")
    private UUID reportDocumentId;

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
    public BigDecimal getValuationAmount() { return valuationAmount; }
    public void setValuationAmount(BigDecimal valuationAmount) { this.valuationAmount = valuationAmount; }
    public BigDecimal getLiquidationAmount() { return liquidationAmount; }
    public void setLiquidationAmount(BigDecimal liquidationAmount) { this.liquidationAmount = liquidationAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public OffsetDateTime getValuatedAt() { return valuatedAt; }
    public void setValuatedAt(OffsetDateTime valuatedAt) { this.valuatedAt = valuatedAt; }
    public String getVendorName() { return vendorName; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public String getVendorInn() { return vendorInn; }
    public void setVendorInn(String vendorInn) { this.vendorInn = vendorInn; }
    public UUID getReportDocumentId() { return reportDocumentId; }
    public void setReportDocumentId(UUID reportDocumentId) { this.reportDocumentId = reportDocumentId; }
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
        if (valuatedAt == null) {
            valuatedAt = createdAt;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
