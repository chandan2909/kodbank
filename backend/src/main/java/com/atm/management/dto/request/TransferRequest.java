package com.atm.management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "Amount is required")
        BigDecimal amount,

        @Size(max = 12, message = "Account number is too long")
        String accountNo,

        String otpId,

        String otp,

        @Size(max = 255, message = "Description is too long")
        String description
) {}
