package com.example.tasks.paymentservice.exception;

public class PaymentAuthorizationException extends RuntimeException {
    public PaymentAuthorizationException(String message) {
        super(message);
    }
}
