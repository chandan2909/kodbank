package com.atm.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "signup")
public class Signup {

    @Id
    @Column(name = "formno", length = 10)
    private String formNo;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "fname", nullable = false, length = 100)
    private String fatherName;

    @Column(name = "dob", nullable = false)
    private LocalDate dob;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "marital", nullable = false, length = 20)
    private String marital;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", nullable = false, length = 100)
    private String state;

    @Column(name = "pin", nullable = false, length = 10)
    private String pincode;

    @Column(name = "religion", nullable = false, length = 50)
    private String religion;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "income", nullable = false, length = 50)
    private String income;

    @Column(name = "education", nullable = false, length = 50)
    private String education;

    @Column(name = "occupation", nullable = false, length = 50)
    private String occupation;

    @Column(name = "pan", nullable = false, length = 20, unique = true)
    private String pan;

    @Column(name = "aadhar", nullable = false, length = 20, unique = true)
    private String aadhar;

    @Column(name = "senior", nullable = false, length = 5)
    private String senior;

    @Column(name = "existing", nullable = false, length = 5)
    private String existing;

    @Column(name = "account_type", nullable = false, length = 50)
    private String accountType;

    @Column(name = "facilities", length = 255)
    private String facilities;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public String getFormNo() { return formNo; }
    public void setFormNo(String formNo) { this.formNo = formNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMarital() { return marital; }
    public void setMarital(String marital) { this.marital = marital; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIncome() { return income; }
    public void setIncome(String income) { this.income = income; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }
    public String getAadhar() { return aadhar; }
    public void setAadhar(String aadhar) { this.aadhar = aadhar; }
    public String getSenior() { return senior; }
    public void setSenior(String senior) { this.senior = senior; }
    public String getExisting() { return existing; }
    public void setExisting(String existing) { this.existing = existing; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getFacilities() { return facilities; }
    public void setFacilities(String facilities) { this.facilities = facilities; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
