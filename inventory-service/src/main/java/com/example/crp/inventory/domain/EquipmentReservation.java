package com.example.crp.inventory.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "equipment_reservation")
public class EquipmentReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true)
    private Long requestId;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(nullable = false)
    private String status; // ACTIVE, RELEASED

    private String reason;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    public Long getId() { return id; }

    public Long getRequestId() { return requestId; }

    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Long getEquipmentId() { return equipmentId; }

    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }

    public void setReason(String reason) { this.reason = reason; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getReleasedAt() { return releasedAt; }

    public void setReleasedAt(OffsetDateTime releasedAt) { this.releasedAt = releasedAt; }
}

