package com.example.crp.inventory.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "equipment_custody_history")
public class EquipmentCustodyHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "custodian", length = 128)
    private String custodian;

    @Column(name = "from_ts", nullable = false)
    private OffsetDateTime fromTs;

    @Column(name = "to_ts")
    private OffsetDateTime toTs;

    @Column(name = "reason", length = 128)
    private String reason;

    public Long getId() { return id; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public String getCustodian() { return custodian; }
    public void setCustodian(String custodian) { this.custodian = custodian; }
    public OffsetDateTime getFromTs() { return fromTs; }
    public void setFromTs(OffsetDateTime fromTs) { this.fromTs = fromTs; }
    public OffsetDateTime getToTs() { return toTs; }
    public void setToTs(OffsetDateTime toTs) { this.toTs = toTs; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @PrePersist
    public void prePersist() {
        if (fromTs == null) {
            fromTs = OffsetDateTime.now();
        }
    }
}
