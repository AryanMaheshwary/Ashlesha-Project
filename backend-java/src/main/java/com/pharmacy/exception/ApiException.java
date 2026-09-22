package com.pharmacy.exception;

/** Base for all application exceptions that map to a specific HTTP status. */
public class ApiException extends RuntimeException {
    public final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }
}
