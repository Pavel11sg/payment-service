package com.example.tasks.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.example.tasks.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponseDto {
    private String id;
    private String orderId;
    private String userId;
    private PaymentStatus status;
    private LocalDateTime timestamp;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String processorTransactionId;
    private String errorCode;
    private String errorMessage;
}
