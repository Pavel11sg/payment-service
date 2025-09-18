package com.example.tasks.paymentservice.repository;

import com.example.tasks.paymentservice.model.Payment;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
	List<Payment> findByOrderId(String orderId);

	List<Payment> findByUserId(String userId);

	List<Payment> findByStatus(String status);

	List<Payment> findAllByStatusIn(List<String> statuses);

	@Query("{$and: ["
			+ "{'timestamp': {$gte: ?0,$lte: ?1}},"
			+ "{'status': {$eq: ?2}}"
			+ "]}")
	List<Payment> findByDateRangeAndStatus(LocalDate startDate, LocalDate endDate, String status);

	@Aggregation(pipeline = {
			"{'$match': {'timestamp': {$gte: ?0, $lte: ?1}}}",
			"{'$group': {'_id': null, 'totalAmount': {'$sum': {'$toDouble': '$payment_amount'}}}}"
	})
	Optional<Double> sumPaymentAmountByPeriod(LocalDate startDate, LocalDate endDate);
}
