package com.example.crp.app.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name="application")
public class Application {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private Long customerId; private Long equipmentId; private Double amount; private Integer termMonths; private Double rateAnnualPct; private String status; private OffsetDateTime createdAt;
  public Long getId(){return id;} public void setId(Long id){this.id=id;} public Long getCustomerId(){return customerId;} public void setCustomerId(Long v){this.customerId=v;} public Long getEquipmentId(){return equipmentId;} public void setEquipmentId(Long v){this.equipmentId=v;} public Double getAmount(){return amount;} public void setAmount(Double v){this.amount=v;} public Integer getTermMonths(){return termMonths;} public void setTermMonths(Integer v){this.termMonths=v;} public Double getRateAnnualPct(){return rateAnnualPct;} public void setRateAnnualPct(Double v){this.rateAnnualPct=v;} public String getStatus(){return status;} public void setStatus(String s){this.status=s;} public OffsetDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(OffsetDateTime v){this.createdAt=v;}
}

