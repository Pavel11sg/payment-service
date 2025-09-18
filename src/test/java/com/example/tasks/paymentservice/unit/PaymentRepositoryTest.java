package com.example.tasks.paymentservice.unit;

import com.example.tasks.paymentservice.TestContainerConfig;
import com.example.tasks.paymentservice.model.Payment;
import com.example.tasks.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
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
		payment1 = createPayment("order1", "user1", "COMPLETED",
				LocalDate.of(2024, 1, 15), new BigDecimal("100.50"));
		payment2 = createPayment("order2", "user1", "PENDING",
				LocalDate.of(2024, 1, 20), new BigDecimal("200.00"));
		payment3 = createPayment("order3", "user2", "COMPLETED",
				LocalDate.of(2024, 2, 1), new BigDecimal("150.75"));
		payment4 = createPayment("order4", "user3", "FAILED",
				LocalDate.of(2024, 2, 10), new BigDecimal("300.25"));
		List<Payment> saved = paymentRepository.saveAll(List.of(payment1, payment2, payment3, payment4));
		System.out.println("Saved payments: " + saved);
	}

	private Payment createPayment(String orderId, String userId, String status,
								  LocalDate date, BigDecimal amount) {
		Payment payment = new Payment();
		payment.setOrderId(orderId);
		payment.setUserId(userId);
		payment.setStatus(status);
		payment.setTimestamp(date);
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
	void findAllByStatusIn_ShouldReturnPaymentsWithMatchingStatuses() {
		// When
		List<Payment> result = paymentRepository.findAllByStatusIn(List.of("COMPLETED", "PENDING"));
		// Then
		assertThat(result).hasSize(3);
		assertThat(result).extracting(Payment::getStatus)
				.containsExactlyInAnyOrder("COMPLETED", "PENDING", "COMPLETED");
	}

	@Test
	void findByDateRangeAndStatus_ShouldReturnPaymentsInDateRangeWithSpecificStatus() {
		// Given
		LocalDate startDate = LocalDate.of(2024, 1, 1);
		LocalDate endDate = LocalDate.of(2024, 1, 31);
		String status = "COMPLETED";
		// When
		List<Payment> result = paymentRepository.findByDateRangeAndStatus(startDate, endDate, status);
		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getOrderId()).isEqualTo("order1");
		assertThat(result.get(0).getStatus()).isEqualTo("COMPLETED");
	}

	@Test
	void findByDateRangeAndStatus_ShouldReturnEmptyListWhenNoMatches() {
		// Given
		LocalDate startDate = LocalDate.of(2024, 3, 1);
		LocalDate endDate = LocalDate.of(2024, 3, 31);
		String status = "COMPLETED";
		// When
		List<Payment> result = paymentRepository.findByDateRangeAndStatus(startDate, endDate, status);
		// Then
		assertThat(result).isEmpty();
	}

	@Test
	void sumPaymentAmountByPeriod_ShouldReturnTotalAmountForDateRange() {
		// Given
		LocalDate startDate = LocalDate.of(2024, 1, 1);
		LocalDate endDate = LocalDate.of(2024, 1, 31);
		// When
		Optional<Double> result = paymentRepository.sumPaymentAmountByPeriod(startDate, endDate);
		// Then
		assertThat(result).isPresent();
		assertThat(result.get()).isEqualByComparingTo(Double.valueOf(300.5));
	}

	@Test
	void sumPaymentAmountByPeriod_ShouldReturnEmptyForEmptyPeriod() {
		// Given
		LocalDate startDate = LocalDate.of(2024, 3, 1);
		LocalDate endDate = LocalDate.of(2024, 3, 31);
		// When
		Optional<Double> result = paymentRepository.sumPaymentAmountByPeriod(startDate, endDate);
		// Then
		assertThat(result).isEmpty();
	}

	@Test
	void sumPaymentAmountByPeriod_ShouldIncludeOnlyPaymentsInDateRange() {
		// Given
		LocalDate startDate = LocalDate.of(2024, 2, 1);
		LocalDate endDate = LocalDate.of(2024, 2, 15);
		// When
		Optional<Double> result = paymentRepository.sumPaymentAmountByPeriod(startDate, endDate);
		// Then
		assertThat(result).isPresent();
		// Sum of payment3 (150.75) + payment4 (300.25) = 451.00
		assertThat(result.get()).isEqualTo(451.00);
	}
}