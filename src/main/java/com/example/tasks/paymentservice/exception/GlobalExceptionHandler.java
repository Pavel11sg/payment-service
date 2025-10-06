package com.example.tasks.paymentservice.exception;

import com.example.tasks.paymentservice.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentAuthorizationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthorization(PaymentAuthorizationException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDto(
                        HttpStatus.FORBIDDEN.value(),
                        "VALIDATION_ACCESS_ERROR",
                        ex.getMessage(),
                        request.getDescription(false).replace("uri=", "")
                ));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handlePaymentNotFound(PaymentNotFoundException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto(
                        HttpStatus.NOT_FOUND.value(),
                        "PAYMENT_NOT_FOUND",
                        ex.getMessage(),
                        request.getDescription(false).replace("uri=", "")
                ));
    }
}
