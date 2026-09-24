package com.atm.management.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LimitRequest(
        @NotNull(message = "Daily limit is required")
        BigDecimal dailyLimit
) {}
