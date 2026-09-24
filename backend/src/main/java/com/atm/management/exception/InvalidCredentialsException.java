package com.atm.management.exception;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String message) {
        super(401, message);
    }
}
