package com.atm.management.dto.response;

import java.time.LocalDateTime;

public record BeneficiaryResponse(
        Long id,
        String accountNo,
        String name,
        String nickname,
        String status,
        LocalDateTime createdAt
) {}
