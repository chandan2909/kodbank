package com.atm.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StatementResponse(
        String maskedCardNumber,
        BigDecimal balance,
        List<TransactionItem> transactions,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public StatementResponse(
            String maskedCardNumber,
            BigDecimal balance,
            List<TransactionItem> transactions) {
        this(maskedCardNumber, balance, transactions,
                0, transactions == null ? 0 : transactions.size(),
                transactions == null ? 0 : transactions.size(),
                transactions != null && !transactions.isEmpty() ? 1 : 0);
    }

    public record TransactionItem(
            LocalDateTime date,
            String type,
            BigDecimal amount,
            String direction
    ) {
        public TransactionItem(LocalDateTime date, String type, BigDecimal amount) {
            this(date, type, amount, "C".equals(type) || "Deposit".equals(type) ? "C" : "D");
        }
    }
}
