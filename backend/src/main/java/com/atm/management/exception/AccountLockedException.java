package com.atm.management.exception;

public class AccountLockedException extends ApiException {

    public AccountLockedException(String message) {
        super(403, message);
    }
}
