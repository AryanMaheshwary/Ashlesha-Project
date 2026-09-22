package com.pharmacy.exception;

public class MissingPrescriptionException extends ApiException {
    public MissingPrescriptionException(String message) {
        super(422, message);
    }
}
