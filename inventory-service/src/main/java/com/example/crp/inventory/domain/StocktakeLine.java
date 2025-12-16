package com.example.crp.inventory.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "stocktake_line")
public class StocktakeLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stocktake_id", nullable = false)
    private Long stocktakeId;

    @Column(name = "equipment_id")
    private Long equipmentId;

    @Column(name = "inventory_number", length = 64)
    private String inventoryNumber;

    @Column(name = "serial_number", length = 128)
    private String serialNumber;

    @Column(name = "expected_location_id")
    private Long expectedLocationId;

    @Column(name = "expected_status", length = 64)
    private String expectedStatus;

    @Column(name = "expected_responsible", length = 128)
    private String expectedResponsible;

    @Column(name = "counted_present")
    private Boolean countedPresent;

    @Column(name = "counted_location_id")
    private Long countedLocationId;

    @Column(name = "counted_status", length = 64)
    private String countedStatus;

    @Column(name = "counted_by", length = 128)
    private String countedBy;

    @Column(name = "counted_at")
    private OffsetDateTime countedAt;

    @Column(name = "note", length = 256)
    private String note;

    public Long getId() { return id; }
    public Long getStocktakeId() { return stocktakeId; }
    public void setStocktakeId(Long stocktakeId) { this.stocktakeId = stocktakeId; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getInventoryNumber() { return inventoryNumber; }
    public void setInventoryNumber(String inventoryNumber) { this.inventoryNumber = inventoryNumber; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public Long getExpectedLocationId() { return expectedLocationId; }
    public void setExpectedLocationId(Long expectedLocationId) { this.expectedLocationId = expectedLocationId; }
    public String getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(String expectedStatus) { this.expectedStatus = expectedStatus; }
    public String getExpectedResponsible() { return expectedResponsible; }
    public void setExpectedResponsible(String expectedResponsible) { this.expectedResponsible = expectedResponsible; }
    public Boolean getCountedPresent() { return countedPresent; }
    public void setCountedPresent(Boolean countedPresent) { this.countedPresent = countedPresent; }
    public Long getCountedLocationId() { return countedLocationId; }
    public void setCountedLocationId(Long countedLocationId) { this.countedLocationId = countedLocationId; }
    public String getCountedStatus() { return countedStatus; }
    public void setCountedStatus(String countedStatus) { this.countedStatus = countedStatus; }
    public String getCountedBy() { return countedBy; }
    public void setCountedBy(String countedBy) { this.countedBy = countedBy; }
    public OffsetDateTime getCountedAt() { return countedAt; }
    public void setCountedAt(OffsetDateTime countedAt) { this.countedAt = countedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

