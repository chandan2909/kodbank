package com.atm.management.dto.response;

import java.math.BigDecimal;

public record TransactionResponse(
        String message,
        BigDecimal newBalance
) {}
