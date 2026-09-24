package com.atm.management.dto.response;

public record RefreshResponse(
        String token,
        String refreshToken,
        String cardNumber,
        String username,
        String role,
        long expiresIn
) {}
