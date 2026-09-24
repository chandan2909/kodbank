package com.atm.management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100)
        String name,

        @NotBlank(message = "Father's Name is required")
        @Size(max = 100)
        String fatherName,

        @NotBlank(message = "Date of Birth is required")
        String dob,

        @NotBlank(message = "Gender is required")
        String gender,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100)
        String email,

        @NotBlank(message = "Marital Status is required")
        String marital,

        @NotBlank(message = "Address is required")
        @Size(max = 255)
        String address,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100)
        String state,

        @NotBlank(message = "PIN Code is required")
        @Pattern(regexp = "\\d{6}", message = "PIN Code must be 6 digits")
        String pincode,

        @NotBlank(message = "Religion is required")
        String religion,

        @NotBlank(message = "Category is required")
        String category,

        @NotBlank(message = "Income is required")
        String income,

        @NotBlank(message = "Education is required")
        String education,

        @NotBlank(message = "Occupation is required")
        String occupation,

        @NotBlank(message = "PAN Number is required")
        @Size(max = 10)
        String pan,

        @NotBlank(message = "Aadhar Number is required")
        @Pattern(regexp = "\\d{12}", message = "Aadhar Number must be 12 digits")
        String aadhar,

        @NotBlank(message = "Senior Citizen status is required")
        String seniorCitizen,

        @NotBlank(message = "Existing Account status is required")
        String existingAccount,

        @NotBlank(message = "Account Type is required")
        String accountType,

        @Size(max = 255)
        String facilities,

        @NotBlank(message = "Security Question is required")
        String securityQuestion,

        @NotBlank(message = "Security answer cannot be empty")
        @Size(max = 255)
        String securityAnswer
) {
    public LocalDate parsedDob() {
        return LocalDate.parse(dob);
    }
}
