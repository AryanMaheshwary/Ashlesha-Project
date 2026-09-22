package com.pharmacy.exception;

public class ExpiredBatchException extends ApiException {
    public ExpiredBatchException(String message) {
        super(409, message);
    }
}
