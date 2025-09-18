package com.example.tasks.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentRequestDto {
	@NotBlank(message = "Payment method token is required")
	private String paymentMethodToken;

	@NotNull(message = "Amount is required")
	@Positive(message = "Amount must be positive")
	@DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
	private BigDecimal amount;

	@NotBlank(message = "Currency is required")
	@Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
	private String currency;

	@NotBlank(message = "Order ID is required")
	private String orderId;
	@NotBlank(message = "userId is required")
	private String userId;

	private String description;
}
