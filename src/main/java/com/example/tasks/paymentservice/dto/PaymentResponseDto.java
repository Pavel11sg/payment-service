package com.example.tasks.paymentservice.dto;

import com.example.tasks.paymentservice.model.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
@Getter
@Setter
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
