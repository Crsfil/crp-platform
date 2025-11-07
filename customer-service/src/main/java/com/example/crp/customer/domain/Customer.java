package com.example.crp.customer.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "customer")
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type; // LEGAL/INDIVIDUAL
    private String name;
    private String inn; // tax id (for legal)
    private String ogrn; // registration
    private String kycStatus; // PENDING/PASSED/FAILED

    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getType(){return type;} public void setType(String type){this.type=type;}
    public String getName(){return name;} public void setName(String name){this.name=name;}
    public String getInn(){return inn;} public void setInn(String inn){this.inn=inn;}
    public String getOgrn(){return ogrn;} public void setOgrn(String ogrn){this.ogrn=ogrn;}
    public String getKycStatus(){return kycStatus;} public void setKycStatus(String s){this.kycStatus=s;}
}

