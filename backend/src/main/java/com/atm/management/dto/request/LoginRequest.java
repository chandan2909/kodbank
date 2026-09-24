package com.atm.management.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Pattern(regexp = "\\d{16}", message = "Card Number must be exactly 16 digits")
        String cardNumber,

        @Size(max = 50, message = "Username must be at most 50 characters")
        String username,

        @jakarta.validation.constraints.NotBlank(message = "PIN is required")
        @Size(min = 4, max = 6, message = "PIN must be between 4 and 6 characters")
        String pin
) {}
