package com.pharmacy.exception;

public class AuthException extends ApiException {
    public AuthException(String message) {
        super(401, message);
    }
}
