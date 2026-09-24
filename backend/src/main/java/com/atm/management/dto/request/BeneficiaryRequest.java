package com.atm.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BeneficiaryRequest(
        @NotBlank(message = "Account number is required")
        @Size(min = 10, max = 12, message = "Account number must be 10–12 digits")
        String accountNo,

        @Size(max = 100, message = "Nickname is too long")
        String nickname
) {}
