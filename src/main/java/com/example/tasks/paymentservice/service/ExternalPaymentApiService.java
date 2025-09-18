package com.example.tasks.paymentservice.service;


import com.example.tasks.paymentservice.model.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.Random;

@Slf4j
@Service
public class ExternalPaymentApiService {

//	private final WebClient webClient;
	private final Random random = new Random();

//	public ExternalPaymentApiService(WebClient webClient) {
//		this.webClient = webClient;
//	}

	public PaymentStatus processPayment() {
		return null;
	}


	public String generateProcessorTransactionId() {
		return "proc_tx_" + System.currentTimeMillis() + "_" + random.nextInt(1000);
	}
}
