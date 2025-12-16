package com.example.crp.inventory.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "equipment")
public class Equipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;
    private String type;
    private String manufacturer;
    private String model;
    private String status;
    private String condition;
    private BigDecimal price;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Column(name = "inventory_number")
    private String inventoryNumber;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "responsible_username")
    private String responsibleUsername;

    @Column(name = "source_receipt_id")
    private Long sourceReceiptId;

    @Column(name = "source_purchase_order_id")
    private Long sourcePurchaseOrderId;

    @Column(name = "source_request_id")
    private Long sourceRequestId;

    @Column(name = "source_supplier_id")
    private Long sourceSupplierId;

    @Column(name = "source_purchase_order_line_id")
    private Long sourcePurchaseOrderLineId;

    @Column(name = "source_request_line_id")
    private Long sourceRequestLineId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVersion() { return version; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getInventoryNumber() { return inventoryNumber; }
    public void setInventoryNumber(String inventoryNumber) { this.inventoryNumber = inventoryNumber; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public String getResponsibleUsername() { return responsibleUsername; }
    public void setResponsibleUsername(String responsibleUsername) { this.responsibleUsername = responsibleUsername; }
    public Long getSourceReceiptId() { return sourceReceiptId; }
    public void setSourceReceiptId(Long sourceReceiptId) { this.sourceReceiptId = sourceReceiptId; }
    public Long getSourcePurchaseOrderId() { return sourcePurchaseOrderId; }
    public void setSourcePurchaseOrderId(Long sourcePurchaseOrderId) { this.sourcePurchaseOrderId = sourcePurchaseOrderId; }
    public Long getSourceRequestId() { return sourceRequestId; }
    public void setSourceRequestId(Long sourceRequestId) { this.sourceRequestId = sourceRequestId; }
    public Long getSourceSupplierId() { return sourceSupplierId; }
    public void setSourceSupplierId(Long sourceSupplierId) { this.sourceSupplierId = sourceSupplierId; }
    public Long getSourcePurchaseOrderLineId() { return sourcePurchaseOrderLineId; }
    public void setSourcePurchaseOrderLineId(Long sourcePurchaseOrderLineId) { this.sourcePurchaseOrderLineId = sourcePurchaseOrderLineId; }
    public Long getSourceRequestLineId() { return sourceRequestLineId; }
    public void setSourceRequestLineId(Long sourceRequestLineId) { this.sourceRequestLineId = sourceRequestLineId; }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
