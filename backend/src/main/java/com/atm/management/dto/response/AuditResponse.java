package com.atm.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AuditResponse(
        List<AuditItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public record AuditItem(
            Long id,
            LocalDateTime date,
            String type,
            String direction,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String accountNo,
            String holderName,
            String maskedCard,
            String description,
            String initiatedBy
    ) {
    }
}
