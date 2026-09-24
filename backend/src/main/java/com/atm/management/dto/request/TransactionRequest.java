package com.atm.management.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull(message = "Amount is required")
        BigDecimal amount,

        String source,

        String otpId,

        String otp
) {}
