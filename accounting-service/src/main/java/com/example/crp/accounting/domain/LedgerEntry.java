package com.example.crp.accounting.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name="ledger_entry")
public class LedgerEntry {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private String type; // INVOICE_ISSUED / PAYMENT_RECEIVED / INVOICE_PAID
  private Long refId; // invoiceId or paymentId
  private Double amount;
  private OffsetDateTime createdAt;
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getType(){return type;} public void setType(String v){this.type=v;}
  public Long getRefId(){return refId;} public void setRefId(Long v){this.refId=v;}
  public Double getAmount(){return amount;} public void setAmount(Double v){this.amount=v;}
  public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){this.createdAt=v;}
}

