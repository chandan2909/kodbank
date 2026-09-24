package com.atm.management.dto.response;

import java.math.BigDecimal;

public record AdminAccountResponse(
        Long accountId,
        String accountNo,
        String holderName,
        BigDecimal balance,
        BigDecimal dailyLimit,
        String status,
        String cardNumber,
        int failedAttempts,
        boolean locked
) {}
