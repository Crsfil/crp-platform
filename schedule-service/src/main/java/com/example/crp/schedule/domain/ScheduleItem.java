package com.example.crp.schedule.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "schedule_item")
public class ScheduleItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long agreementId;
    private LocalDate dueDate;
    private Double amount;
    private String status; // PLANNED/INVOICED/PAID

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getAgreementId(){return agreementId;} public void setAgreementId(Long v){this.agreementId=v;}
    public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){this.dueDate=v;}
    public Double getAmount(){return amount;} public void setAmount(Double v){this.amount=v;}
    public String getStatus(){return status;} public void setStatus(String v){this.status=v;}
}

