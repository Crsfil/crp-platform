package com.example.crp.inventory.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "equipment_disposition")
public class EquipmentDisposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "type", nullable = false, length = 32)
    private String type; // SALE, DISPOSAL

    @Column(name = "status", nullable = false, length = 32)
    private String status; // DRAFT, APPROVED, COMPLETED, CANCELED

    @Column(name = "planned_price", precision = 18, scale = 2)
    private BigDecimal plannedPrice;

    @Column(name = "actual_price", precision = 18, scale = 2)
    private BigDecimal actualPrice;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "counterparty_name", length = 256)
    private String counterpartyName;

    @Column(name = "counterparty_inn", length = 32)
    private String counterpartyInn;

    @Column(name = "sale_method", length = 32)
    private String saleMethod; // DIRECT, AUCTION

    @Column(name = "lot_number", length = 64)
    private String lotNumber;

    @Column(name = "contract_number", length = 64)
    private String contractNumber;

    @Column(name = "invoice_number", length = 64)
    private String invoiceNumber;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "performed_at")
    private OffsetDateTime performedAt;

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
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getPlannedPrice() { return plannedPrice; }
    public void setPlannedPrice(BigDecimal plannedPrice) { this.plannedPrice = plannedPrice; }
    public BigDecimal getActualPrice() { return actualPrice; }
    public void setActualPrice(BigDecimal actualPrice) { this.actualPrice = actualPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCounterpartyName() { return counterpartyName; }
    public void setCounterpartyName(String counterpartyName) { this.counterpartyName = counterpartyName; }
    public String getCounterpartyInn() { return counterpartyInn; }
    public void setCounterpartyInn(String counterpartyInn) { this.counterpartyInn = counterpartyInn; }
    public String getSaleMethod() { return saleMethod; }
    public void setSaleMethod(String saleMethod) { this.saleMethod = saleMethod; }
    public String getLotNumber() { return lotNumber; }
    public void setLotNumber(String lotNumber) { this.lotNumber = lotNumber; }
    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public OffsetDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(OffsetDateTime paidAt) { this.paidAt = paidAt; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public OffsetDateTime getPerformedAt() { return performedAt; }
    public void setPerformedAt(OffsetDateTime performedAt) { this.performedAt = performedAt; }
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
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
