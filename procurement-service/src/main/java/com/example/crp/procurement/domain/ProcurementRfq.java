package com.example.crp.procurement.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procurement_rfq")
public class ProcurementRfq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_type", nullable = false, length = 64)
    private String serviceType; // SERVICE_EVICTION, SERVICE_STORAGE, SERVICE_VALUATION, SERVICE_REPAIR, SERVICE_AUCTION

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "title", length = 256)
    private String title;

    @Column(name = "status", nullable = false, length = 32)
    private String status; // OPEN, AWARDED, CLOSED, CANCELED

    @Column(name = "awarded_supplier_id")
    private Long awardedSupplierId;

    @Column(name = "award_reason", length = 512)
    private String awardReason;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "rfq", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcurementRfqOffer> offers = new ArrayList<>();

    public Long getId() { return id; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getAwardedSupplierId() { return awardedSupplierId; }
    public void setAwardedSupplierId(Long awardedSupplierId) { this.awardedSupplierId = awardedSupplierId; }
    public String getAwardReason() { return awardReason; }
    public void setAwardReason(String awardReason) { this.awardReason = awardReason; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<ProcurementRfqOffer> getOffers() { return offers; }
    public void setOffers(List<ProcurementRfqOffer> offers) { this.offers = offers; }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
        if (status == null || status.isBlank()) status = "OPEN";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
