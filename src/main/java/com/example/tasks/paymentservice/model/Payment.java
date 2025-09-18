package com.example.tasks.paymentservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Document(collection = "payments")
@ToString
public class Payment {
	@Id
	private String id;

	@Field("order_id")
	private String orderId;

	@Field("user_id")
	private String userId;

	@Field(value = "status", targetType = FieldType.STRING)
	private PaymentStatus status;

	@Field("timestamp")
	private LocalDateTime timestamp;

	@Field("payment_amount")
	private BigDecimal paymentAmount;

	@Field("currency")
	private String currency;

	@Field("payment_method_token")
	private String paymentMethodToken;

	@Field("processor_transaction_id")
	private String processorTransactionId;

	@Field("error_code")
	private String errorCode;

	@Field("error_message")
	private String errorMessage;

	@Field("description")
	private String description;
}



