package com.atm.management.dto.response;

import java.math.BigDecimal;

public record BalanceResponse(
        BigDecimal balance,
        String currency
) {}
