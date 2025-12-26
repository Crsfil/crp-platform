package com.example.crp.agreement.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity @Table(name="agreement")
public class Agreement {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private Long applicationId;
  private String number;
  private String status;
  private OffsetDateTime signedAt;
  private Integer termMonths;
  private Double rateAnnualPct;
  private Integer restructureVersion;
  private OffsetDateTime restructuredAt;
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public Long getApplicationId(){return applicationId;} public void setApplicationId(Long v){this.applicationId=v;}
  public String getNumber(){return number;} public void setNumber(String v){this.number=v;}
  public String getStatus(){return status;} public void setStatus(String v){this.status=v;}
  public OffsetDateTime getSignedAt(){return signedAt;} public void setSignedAt(OffsetDateTime v){this.signedAt=v;}
  public Integer getTermMonths(){return termMonths;} public void setTermMonths(Integer v){this.termMonths=v;}
  public Double getRateAnnualPct(){return rateAnnualPct;} public void setRateAnnualPct(Double v){this.rateAnnualPct=v;}
  public Integer getRestructureVersion(){return restructureVersion;} public void setRestructureVersion(Integer v){this.restructureVersion=v;}
  public OffsetDateTime getRestructuredAt(){return restructuredAt;} public void setRestructuredAt(OffsetDateTime v){this.restructuredAt=v;}
}
