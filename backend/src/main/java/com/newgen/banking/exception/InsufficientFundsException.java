package com.newgen.banking.exception;


 //Thrown when an account has insufficient balance for a withdrawal or debit.

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
