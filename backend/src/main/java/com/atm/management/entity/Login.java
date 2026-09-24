package com.atm.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "login")
public class Login {

    @Id
    @Column(name = "cardno", length = 16)
    private String cardNo;

    @Column(name = "username", length = 50, unique = true)
    private String username;

    @Column(name = "formno", length = 10)
    private String formNo;

    @Column(name = "pin", nullable = false, length = 100)
    private String pin;

    @Column(name = "failed_attempts")
    private int failedAttempts;

    @Column(name = "is_locked")
    private int locked;

    @Column(name = "security_question", length = 255)
    private String securityQuestion;

    @Column(name = "security_answer", length = 255)
    private String securityAnswer;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "role", length = 10)
    private String role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public boolean isLocked() {
        return locked == 1;
    }

    public void lock() {
        this.locked = 1;
    }

    public void unlock() {
        this.locked = 0;
    }

    public String getCardNo() { return cardNo; }
    public void setCardNo(String cardNo) { this.cardNo = cardNo; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFormNo() { return formNo; }
    public void setFormNo(String formNo) { this.formNo = formNo; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public int getLocked() { return locked; }
    public void setLocked(int locked) { this.locked = locked; }
    public String getSecurityQuestion() { return securityQuestion; }
    public void setSecurityQuestion(String securityQuestion) { this.securityQuestion = securityQuestion; }
    public String getSecurityAnswer() { return securityAnswer; }
    public void setSecurityAnswer(String securityAnswer) { this.securityAnswer = securityAnswer; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
