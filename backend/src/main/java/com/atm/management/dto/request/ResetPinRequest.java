package com.atm.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPinRequest(
        @NotBlank(message = "Card Number is required")
        @Pattern(regexp = "\\d{16}", message = "Card Number must be exactly 16 digits")
        String cardNumber,

        @NotBlank(message = "Security answer cannot be empty")
        @Size(max = 255)
        String securityAnswer,

        @NotBlank(message = "New PIN is required")
        String newPin,

        @NotBlank(message = "Confirm PIN is required")
        String confirmPin
) {}
