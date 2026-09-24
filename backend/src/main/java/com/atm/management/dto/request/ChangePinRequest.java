package com.atm.management.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangePinRequest(
        @NotBlank(message = "New PIN is required")
        String newPin,

        @NotBlank(message = "Confirm PIN is required")
        String confirmPin
) {}
