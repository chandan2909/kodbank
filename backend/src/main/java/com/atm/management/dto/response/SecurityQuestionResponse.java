package com.atm.management.dto.response;

public record SecurityQuestionResponse(
        String cardNumber,
        String securityQuestion
) {}
