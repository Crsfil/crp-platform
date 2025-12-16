package com.example.crp.inventory.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "equipment_lease")
public class EquipmentLease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "agreement_id")
    private Long agreementId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "issued_from_location_id")
    private Long issuedFromLocationId;

    @Column(name = "issued_to_location_id")
    private Long issuedToLocationId;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // ACTIVE, CLOSED, DEFAULTED

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "expected_return_at")
    private OffsetDateTime expectedReturnAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

    @Column(name = "repossessed_at")
    private OffsetDateTime repossessedAt;

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
    public Long getAgreementId() { return agreementId; }
    public void setAgreementId(Long agreementId) { this.agreementId = agreementId; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getIssuedFromLocationId() { return issuedFromLocationId; }
    public void setIssuedFromLocationId(Long issuedFromLocationId) { this.issuedFromLocationId = issuedFromLocationId; }
    public Long getIssuedToLocationId() { return issuedToLocationId; }
    public void setIssuedToLocationId(Long issuedToLocationId) { this.issuedToLocationId = issuedToLocationId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getStartAt() { return startAt; }
    public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }
    public OffsetDateTime getExpectedReturnAt() { return expectedReturnAt; }
    public void setExpectedReturnAt(OffsetDateTime expectedReturnAt) { this.expectedReturnAt = expectedReturnAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }
    public OffsetDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(OffsetDateTime returnedAt) { this.returnedAt = returnedAt; }
    public OffsetDateTime getRepossessedAt() { return repossessedAt; }
    public void setRepossessedAt(OffsetDateTime repossessedAt) { this.repossessedAt = repossessedAt; }
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
        if (startAt == null) {
            startAt = createdAt;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}

