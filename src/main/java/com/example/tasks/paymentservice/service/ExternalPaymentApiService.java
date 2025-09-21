package com.example.tasks.paymentservice.service;

import com.example.tasks.paymentservice.dto.ExternalPaymentApiResponse;
import com.example.tasks.paymentservice.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class ExternalPaymentApiService {
	private final Random random = new Random();

	public ExternalPaymentApiResponse processPayment(Payment payment) {
		log.info("Processing payment...\n" + payment);
		ExternalPaymentApiResponse response = new ExternalPaymentApiResponse();
		int number = random.nextInt(100) + 1;
		String transactionId = "proc_tx_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
		response.setPaymentStatusNumber(number);
		response.setTransactionId(transactionId);
		log.info("Status number was: " + number);
		return response;
	}
}
