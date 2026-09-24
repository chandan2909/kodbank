package com.atm.management.dto.response;

public record OtpInitiateResponse(
        String otpId,
        String purpose,
        long ttlSeconds,
        String message,
        String devOtp
) {}
