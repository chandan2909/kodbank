package com.atm.management.dto.response;

import java.util.List;

public record ErrorResponse(
        int status,
        String message,
        List<String> errors
) {
    public ErrorResponse(int status, String message) {
        this(status, message, List.of());
    }
}
