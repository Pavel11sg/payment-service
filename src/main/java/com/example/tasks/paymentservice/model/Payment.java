package com.example.tasks.paymentservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDate;

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
	@Field("status")
	private String status;
	@Field("timestamp")
	private LocalDate timestamp;
	@Field("payment_amount")
	private BigDecimal paymentAmount;
}
