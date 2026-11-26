package com.newgen.banking.exception;

 //Thrown when a request contains invalid domain values.
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
