package com.newgen.banking.exception;

 // Thrown when a uniqueness or state conflict cannot be resolved.
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
