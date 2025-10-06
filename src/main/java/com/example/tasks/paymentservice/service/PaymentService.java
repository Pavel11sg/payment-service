package com.example.tasks.paymentservice.service;

import com.example.tasks.paymentservice.dto.ExternalPaymentApiResponse;
import com.example.tasks.paymentservice.dto.PaymentRequestDto;
import com.example.tasks.paymentservice.dto.PaymentResponseDto;
import com.example.tasks.paymentservice.dto.mapper.PaymentMapper;
import com.example.tasks.paymentservice.exception.PaymentAuthorizationException;
import com.example.tasks.paymentservice.exception.PaymentNotFoundException;
import com.example.tasks.paymentservice.model.Payment;
import com.example.tasks.paymentservice.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.example.tasks.dto.OrderCreatedEvent;
import org.example.tasks.dto.PaymentCreatedEvent;
import org.example.tasks.model.PaymentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ExternalPaymentApiService externalPaymentApiService;
    private final PaymentMapper paymentMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${kafka.topics.payment-created}")
    private String paymentCreatedTopic;

    public PaymentService(PaymentRepository paymentRepository, ExternalPaymentApiService externalPaymentApiService, PaymentMapper paymentMapper, KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.externalPaymentApiService = externalPaymentApiService;
        this.paymentMapper = paymentMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto requestDto, String authenticatedId, String roles) {
        validateAuthorization(requestDto.getUserId(), authenticatedId, roles, "perform");

        Payment payment = paymentMapper.toEntity(requestDto);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTimestamp(LocalDateTime.now());
        try {
            ExternalPaymentApiResponse paymentResponse = externalPaymentApiService.processPayment(payment);
            if (isPaymentSuccessful(paymentResponse)) {
                payment.setStatus(PaymentStatus.SUCCESS);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }
            payment.setProcessorTransactionId(paymentResponse.getTransactionId());
            payment = paymentRepository.save(payment);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            payment.setErrorCode("API_ERROR");
            payment = paymentRepository.save(payment);
        }
        return paymentMapper.toDto(payment);
    }

    @Transactional
    public PaymentResponseDto getPaymentById(String paymentId, String authenticatedId, String roles) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(String.format("Payment with id=%s not found", paymentId)));

        validateAuthorization(payment.getUserId(), authenticatedId, roles, "get");
        return paymentMapper.toDto(payment);
    }

    @Transactional
    public List<PaymentResponseDto> getAllPaymentsByOrderId(String orderId, String authenticatedId, String roles) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);

        if (payments.isEmpty()) {
            return Collections.emptyList();
        }

        if (!isAdmin(roles)) {
            boolean unauthorizedPayment = payments.stream().anyMatch(payment -> !payment.getUserId().equals(String.valueOf(authenticatedId)));
            if (unauthorizedPayment) {
                throw new PaymentAuthorizationException("You are not authorized to access these payments!");
            }
        }

        return payments.stream().map(paymentMapper::toDto).toList();
    }

    @Transactional
    public List<PaymentResponseDto> getAllPaymentsByUserId(String requestedUserId, String authenticatedId, String roles) {
        validateAuthorization(requestedUserId, authenticatedId, roles, "get");
        List<Payment> payments = paymentRepository.findByUserId(requestedUserId);
        return payments.stream().map(paymentMapper::toDto).toList();
    }

    private void validateAuthorization(String targetUserId, String authenticatedId, String roles, String action) {
        if (!hasRequiredRole(roles)) {
            throw new PaymentAuthorizationException("You are not authorized to " + action + " this payment!");
        }
        if (!isAdmin(roles) && !targetUserId.equals(authenticatedId)) {
            throw new PaymentAuthorizationException("You are not authorized to " + action + " this payment!");
        }
    }

    private boolean hasRequiredRole(String roles) {
        return isUser(roles) || isAdmin(roles);
    }

    private boolean isAdmin(String roles) {
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    private boolean isUser(String roles) {
        return roles != null && roles.contains("ROLE_USER");
    }

    private boolean isPaymentSuccessful(ExternalPaymentApiResponse response) {
        return response.getPaymentStatusNumber() % 2 == 0;
    }

    @Transactional
    public void processOrderCreatedEvent(OrderCreatedEvent orderCreatedEvent) {
        log.info("Processing OrderCreatedEvent for order: {}", orderCreatedEvent.getOrderId());

        Payment payment = new Payment();
        payment.setOrderId(orderCreatedEvent.getOrderId().toString());
        payment.setUserId(orderCreatedEvent.getUserId().toString());
        payment.setPaymentAmount(orderCreatedEvent.getTotalAmount());
        payment.setCurrency(orderCreatedEvent.getCurrency());
        payment.setTimestamp(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setDescription("Payment for order: " + orderCreatedEvent.getOrderId());
        payment.setPaymentMethodToken(orderCreatedEvent.getPaymentMethodToken());

        try {
            ExternalPaymentApiResponse paymentResponse = externalPaymentApiService.processPayment(payment);
            if (isPaymentSuccessful(paymentResponse)) {
                payment.setStatus(PaymentStatus.SUCCESS);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setErrorMessage("Payment processing failed");
            }
            payment.setProcessorTransactionId(paymentResponse.getTransactionId());

        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            payment.setErrorCode("PAYMENT_PROCESSING_ERROR");
        }

        Payment savedPayment = paymentRepository.save(payment);
        sendPaymentCreatedEvent(savedPayment);
    }

    private void sendPaymentCreatedEvent(Payment payment) {
        try {
            PaymentCreatedEvent event = new PaymentCreatedEvent();
            event.setOrderId(UUID.fromString(payment.getOrderId()));
            event.setStatus(payment.getStatus());
            event.setPaymentId(payment.getId());
            event.setErrorMessage(payment.getErrorMessage());

            kafkaTemplate.send(paymentCreatedTopic, event);
            log.info("PaymentCreatedEvent sent for order: {}, status: {}",
                    payment.getOrderId(), payment.getStatus());

        } catch (Exception e) {
            log.error("Failed to send PaymentCreatedEvent for payment: {}", payment.getId(), e);
        }
    }
}