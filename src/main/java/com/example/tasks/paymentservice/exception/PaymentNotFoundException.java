package com.example.tasks.paymentservice.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String msg) {
        super(msg);
    }

}
