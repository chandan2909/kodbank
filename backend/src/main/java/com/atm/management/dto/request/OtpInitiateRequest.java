package com.atm.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpInitiateRequest(
        @NotBlank(message = "Purpose is required")
        @Size(max = 20)
        String purpose
) {}
