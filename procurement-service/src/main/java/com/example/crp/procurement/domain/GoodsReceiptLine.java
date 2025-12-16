package com.example.crp.procurement.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "goods_receipt_line")
public class GoodsReceiptLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private GoodsReceipt receipt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_line_id", nullable = false)
    private PurchaseOrderLine purchaseOrderLine;

    @Column(name = "quantity_received", nullable = false)
    private BigDecimal quantityReceived;

    public Long getId() { return id; }
    public GoodsReceipt getReceipt() { return receipt; }
    public void setReceipt(GoodsReceipt receipt) { this.receipt = receipt; }
    public PurchaseOrderLine getPurchaseOrderLine() { return purchaseOrderLine; }
    public void setPurchaseOrderLine(PurchaseOrderLine purchaseOrderLine) { this.purchaseOrderLine = purchaseOrderLine; }
    public BigDecimal getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(BigDecimal quantityReceived) { this.quantityReceived = quantityReceived; }
}

