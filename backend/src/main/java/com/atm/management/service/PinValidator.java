package com.atm.management.service;

import com.atm.management.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

@Component
public class PinValidator {

    private static final String[] SEQUENCES = {
            "1234", "2345", "3456", "4567", "5678", "6789",
            "9876", "8765", "7654", "6543", "5432", "4321"
    };

    public void validate(String pin) {
        if (pin == null || pin.isBlank()) {
            throw new BusinessRuleException("PIN cannot be empty");
        }
        if (!pin.matches("\\d+")) {
            throw new BusinessRuleException("PIN must contain only digits");
        }
        if (pin.length() < 4 || pin.length() > 6) {
            throw new BusinessRuleException("PIN must be between 4 and 6 digits");
        }
        if (hasSequentialDigits(pin)) {
            throw new BusinessRuleException(
                    "PIN must not contain sequential digits (e.g., 1234, 5678)");
        }
        if (hasTooManyRepeatingDigits(pin)) {
            throw new BusinessRuleException(
                    "PIN must not contain too many repeating digits");
        }
    }

    private boolean hasSequentialDigits(String pin) {
        for (String seq : SEQUENCES) {
            if (pin.contains(seq)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasTooManyRepeatingDigits(String pin) {
        for (char c = '0'; c <= '9'; c++) {
            int count = 0;
            for (int i = 0; i < pin.length(); i++) {
                if (pin.charAt(i) == c) {
                    count++;
                }
            }
            if (count > 3) {
                return true;
            }
        }
        for (int i = 0; i < pin.length() - 2; i++) {
            if (pin.charAt(i) == pin.charAt(i + 1) && pin.charAt(i) == pin.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }
}
