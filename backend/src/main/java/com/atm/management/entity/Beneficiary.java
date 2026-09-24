package com.atm.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_account_id", nullable = false)
    private Long ownerAccountId;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "beneficiary_account_no", nullable = false, length = 12)
    private String beneficiaryAccountNo;

    @Column(name = "beneficiary_name", length = 100)
    private String beneficiaryName;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOwnerAccountId() { return ownerAccountId; }
    public void setOwnerAccountId(Long ownerAccountId) { this.ownerAccountId = ownerAccountId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getBeneficiaryAccountNo() { return beneficiaryAccountNo; }
    public void setBeneficiaryAccountNo(String beneficiaryAccountNo) { this.beneficiaryAccountNo = beneficiaryAccountNo; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
