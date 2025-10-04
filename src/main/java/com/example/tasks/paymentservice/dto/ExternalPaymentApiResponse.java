package com.example.tasks.paymentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalPaymentApiResponse {
	private Integer paymentStatusNumber;

	private String transactionId;
}
