package com.newgen.banking.exception;

 // Thrown when attempting to register a customer with an email that already exists.
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
