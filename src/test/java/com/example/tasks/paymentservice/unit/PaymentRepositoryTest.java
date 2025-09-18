package com.example.tasks.paymentservice.unit;


import com.example.tasks.paymentservice.TestContainerConfig;
import com.example.tasks.paymentservice.model.Payment;
import com.example.tasks.paymentservice.model.PaymentStatus;
import com.example.tasks.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
class PaymentRepositoryTest {

	@Autowired
	private PaymentRepository paymentRepository;

	private Payment payment1, payment2, payment3, payment4;

	@BeforeEach
	void setUp() {
		paymentRepository.deleteAll();
		payment1 = createPayment("order1", "user1", PaymentStatus.SUCCESS,
				LocalDateTime.of(2024, 1, 15, 10, 30), new BigDecimal("100.50"));
		payment2 = createPayment("order2", "user1", PaymentStatus.PENDING,
				LocalDateTime.of(2024, 1, 20, 14, 15), new BigDecimal("200.00"));
		payment3 = createPayment("order3", "user2", PaymentStatus.SUCCESS,
				LocalDateTime.of(2024, 2, 1, 9, 0), new BigDecimal("150.75"));
		payment4 = createPayment("order4", "user3", PaymentStatus.FAILED,
				LocalDateTime.of(2024, 2, 10, 16, 45), new BigDecimal("300.25"));
		List<Payment> saved = paymentRepository.saveAll(List.of(payment1, payment2, payment3, payment4));
		System.out.println("Saved payments: " + saved);
	}

	private Payment createPayment(String orderId, String userId, PaymentStatus status,
								  LocalDateTime dateTime, BigDecimal amount) {
		Payment payment = new Payment();
		payment.setOrderId(orderId);
		payment.setUserId(userId);
		payment.setStatus(status);
		payment.setTimestamp(dateTime);
		payment.setPaymentAmount(amount);
		return payment;
	}

	@Test
	void findByOrderId_ShouldReturnPaymentsForSpecificOrder() {
		// When
		List<Payment> result = paymentRepository.findByOrderId("order1");
		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOrderId()).isEqualTo("order1");
		assertThat(result.get(0).getPaymentAmount()).isEqualTo(new BigDecimal("100.50"));
	}

	@Test
	void findByUserId_ShouldReturnPaymentsForSpecificUser() {
		// When
		List<Payment> result = paymentRepository.findByUserId("user1");
		// Then
		assertThat(result).hasSize(2);
		assertThat(result).extracting(Payment::getUserId).containsOnly("user1");
		assertThat(result).extracting(Payment::getOrderId).contains("order1", "order2");
	}

	@Test
	void findByStatus_WithEnum_ShouldReturnPaymentsWithSpecificStatus() {
		// When
		List<Payment> result = paymentRepository.findByStatus(PaymentStatus.SUCCESS);
		// Then
		assertThat(result).hasSize(2);
		assertThat(result).extracting(Payment::getStatus).containsOnly(PaymentStatus.SUCCESS);
	}

	@Test
	void findAllByStatusIn_ShouldReturnPaymentsWithMatchingStatuses() {
		// When
		List<Payment> result = paymentRepository.findAllByStatusIn(List.of(PaymentStatus.SUCCESS, PaymentStatus.PENDING));
		// Then
		assertThat(result).hasSize(3);
		assertThat(result).extracting(Payment::getStatus)
				.containsExactlyInAnyOrder(PaymentStatus.SUCCESS, PaymentStatus.PENDING, PaymentStatus.SUCCESS);
	}

	@Test
	void findByDateRangeAndStatus_ShouldReturnPaymentsInDateRangeWithSpecificStatus() {
		// Given
		LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59);
		PaymentStatus status = PaymentStatus.SUCCESS;
		// When
		List<Payment> result = paymentRepository.findByDateRangeAndStatus(startDate, endDate, status);
		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOrderId()).isEqualTo("order1");
		assertThat(result.get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
	}

	@Test
	void findByDateRangeAndStatus_ShouldReturnEmptyListWhenNoMatches() {
		// Given
		LocalDateTime startDate = LocalDateTime.of(2024, 3, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2024, 3, 31, 23, 59);
		PaymentStatus status = PaymentStatus.SUCCESS;
		// When
		List<Payment> result = paymentRepository.findByDateRangeAndStatus(startDate, endDate, status);
		// Then
		assertThat(result).isEmpty();
	}

	@Test
	void sumPaymentAmountByPeriod_ShouldReturnTotalAmountForDateRange() {
		// Given
		LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59);
		// When
		Optional<Double> result = paymentRepository.sumPaymentAmountByPeriod(startDate, endDate);
		// Then
		assertThat(result).isPresent();
		// payment1 (100.50) + payment2 (200.00) = 300.50
		assertThat(result.get()).isEqualTo(300.50);
	}

	@Test
	void sumPaymentAmountByPeriod_ShouldReturnEmptyForEmptyPeriod() {
		// Given
		LocalDateTime startDate = LocalDateTime.of(2024, 3, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2024, 3, 31, 23, 59);
		// When
		Optional<Double> result = paymentRepository.sumPaymentAmountByPeriod(startDate, endDate);
		// Then
		assertThat(result).isEmpty();
	}

	@Test
	void sumPaymentAmountByPeriod_ShouldIncludeOnlyPaymentsInDateRange() {
		// Given
		LocalDateTime startDate = LocalDateTime.of(2024, 2, 1, 0, 0);
		LocalDateTime endDate = LocalDateTime.of(2024, 2, 15, 23, 59);
		// When
		Optional<Double> result = paymentRepository.sumPaymentAmountByPeriod(startDate, endDate);
		// Then
		assertThat(result).isPresent();
		// Sum of payment3 (150.75) + payment4 (300.25) = 451.00
		assertThat(result.get()).isEqualTo(451.00);
	}
}