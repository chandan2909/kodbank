package com.atm.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FROZEN = "FROZEN";
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_no", nullable = false, length = 12, unique = true)
    private String accountNo;

    @Column(name = "formno", length = 10)
    private String formNo;

    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "daily_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal dailyLimit;

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "is_system", nullable = false)
    private int system;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public boolean isSystem() {
        return system == 1;
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    public String getFormNo() { return formNo; }
    public void setFormNo(String formNo) { this.formNo = formNo; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSystem() { return system; }
    public void setSystem(int system) { this.system = system; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
