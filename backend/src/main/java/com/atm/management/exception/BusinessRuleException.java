package com.atm.management.exception;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(400, message);
    }
}
