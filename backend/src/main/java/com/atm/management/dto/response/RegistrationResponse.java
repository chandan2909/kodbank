package com.atm.management.dto.response;

public record RegistrationResponse(
        String formNo,
        String cardNumber,
        String pin,
        String accountNumber
) {}
