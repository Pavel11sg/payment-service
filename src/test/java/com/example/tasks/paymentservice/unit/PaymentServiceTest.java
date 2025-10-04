package com.example.tasks.paymentservice.unit;

import com.example.tasks.paymentservice.dto.ExternalPaymentApiResponse;
import com.example.tasks.paymentservice.dto.PaymentRequestDto;
import com.example.tasks.paymentservice.dto.PaymentResponseDto;
import com.example.tasks.paymentservice.dto.mapper.PaymentMapper;
import com.example.tasks.paymentservice.exception.PaymentAuthorizationException;
import com.example.tasks.paymentservice.exception.PaymentNotFoundException;
import com.example.tasks.paymentservice.model.Payment;
import com.example.tasks.paymentservice.repository.PaymentRepository;
import com.example.tasks.paymentservice.service.ExternalPaymentApiService;
import com.example.tasks.paymentservice.service.PaymentService;
import org.example.tasks.dto.OrderCreatedEvent;
import org.example.tasks.dto.PaymentCreatedEvent;
import org.example.tasks.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ExternalPaymentApiService externalPaymentApiService;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<Payment> paymentCaptor;

    @Captor
    private ArgumentCaptor<PaymentCreatedEvent> eventCaptor;

    private PaymentRequestDto paymentRequestDto;
    private Payment payment;
    private PaymentResponseDto paymentResponseDto;
    private OrderCreatedEvent orderCreatedEvent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "paymentCreatedTopic", "payment-created-topic");

        paymentRequestDto = new PaymentRequestDto();
        paymentRequestDto.setPaymentMethodToken("pm_token_123");
        paymentRequestDto.setAmount(new BigDecimal("100.50"));
        paymentRequestDto.setCurrency("USD");
        paymentRequestDto.setOrderId("68d6ccaa-bec0-4d1a-a328-f771c5b78d44");
        paymentRequestDto.setUserId("b25c69da-00d6-442a-a922-a8c88ad34b62");
        paymentRequestDto.setDescription("Test payment");

        payment = new Payment();
        payment.setId("payment-123");
        payment.setOrderId("68d6ccaa-bec0-4d1a-a328-f771c5b78d44");
        payment.setUserId("b25c69da-00d6-442a-a922-a8c88ad34b62");
        payment.setPaymentAmount(new BigDecimal("100.50"));
        payment.setCurrency("USD");
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTimestamp(LocalDateTime.now());
        payment.setPaymentMethodToken("pm_token_123");
        payment.setDescription("Test payment");

        paymentResponseDto = new PaymentResponseDto();
        paymentResponseDto.setId("payment-123");
        paymentResponseDto.setOrderId("68d6ccaa-bec0-4d1a-a328-f771c5b78d44");
        paymentResponseDto.setUserId("b25c69da-00d6-442a-a922-a8c88ad34b62");
        paymentResponseDto.setAmount(new BigDecimal("100.50"));
        paymentResponseDto.setCurrency("USD");
        paymentResponseDto.setStatus(PaymentStatus.PENDING);
        paymentResponseDto.setTimestamp(LocalDateTime.now());

        orderCreatedEvent = new OrderCreatedEvent();
        orderCreatedEvent.setOrderId(UUID.fromString("68d6ccaa-bec0-4d1a-a328-f771c5b78d44"));
        orderCreatedEvent.setUserId(UUID.fromString("b25c69da-00d6-442a-a922-a8c88ad34b62"));
        orderCreatedEvent.setTotalAmount(new BigDecimal("150.75"));
        orderCreatedEvent.setCurrency("EUR");
        orderCreatedEvent.setPaymentMethodToken("pm_token_456");
    }

    @Test
    void processPayment_WithSuccessfulExternalPayment_ShouldReturnSuccessResponse() {
        // Given
        String authenticatedId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String roles = "ROLE_USER";
        ExternalPaymentApiResponse apiResponse = new ExternalPaymentApiResponse();
        apiResponse.setPaymentStatusNumber(2);
        apiResponse.setTransactionId("tx_success_123");

        when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);
        when(externalPaymentApiService.processPayment(any(Payment.class))).thenReturn(apiResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        PaymentResponseDto result = paymentService.processPayment(paymentRequestDto, authenticatedId, roles);

        // Then
        assertThat(result).isNotNull();
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(savedPayment.getProcessorTransactionId()).isEqualTo("tx_success_123");
        verify(externalPaymentApiService).processPayment(payment);
    }

    @Test
    void processPayment_WithFailedExternalPayment_ShouldReturnFailedResponse() {
        // Given
        String authenticatedId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String roles = "ROLE_USER";
        ExternalPaymentApiResponse apiResponse = new ExternalPaymentApiResponse();
        apiResponse.setPaymentStatusNumber(3);
        apiResponse.setTransactionId("tx_failed_123");

        when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);
        when(externalPaymentApiService.processPayment(any(Payment.class))).thenReturn(apiResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        PaymentResponseDto result = paymentService.processPayment(paymentRequestDto, authenticatedId, roles);

        // Then
        assertThat(result).isNotNull();
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getProcessorTransactionId()).isEqualTo("tx_failed_123");
    }

    @Test
    void processPayment_WithExternalApiException_ShouldReturnFailedResponseWithError() {
        // Given
        String authenticatedId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String roles = "ROLE_USER";
        RuntimeException apiException = new RuntimeException("External API unavailable");

        when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);
        when(externalPaymentApiService.processPayment(any(Payment.class))).thenThrow(apiException);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        PaymentResponseDto result = paymentService.processPayment(paymentRequestDto, authenticatedId, roles);

        // Then
        assertThat(result).isNotNull();
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getErrorMessage()).isEqualTo("External API unavailable");
        assertThat(savedPayment.getErrorCode()).isEqualTo("API_ERROR");
    }

    @Test
    void processPayment_WithUnauthorizedUser_ShouldThrowAuthorizationException() {
        // Given
        String authenticatedId = "user-456";
        String roles = "ROLE_USER";

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(paymentRequestDto, authenticatedId, roles))
                .isInstanceOf(PaymentAuthorizationException.class)
                .hasMessage("You are not authorized to perform this payment!");

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(externalPaymentApiService, never()).processPayment(any(Payment.class));
    }

    @Test
    void processPayment_WithAdminRole_ShouldProcessPaymentForDifferentUser() {
        // Given
        String authenticatedId = "admin-user";
        String roles = "ROLE_ADMIN";
        ExternalPaymentApiResponse apiResponse = new ExternalPaymentApiResponse();
        apiResponse.setPaymentStatusNumber(2);
        apiResponse.setTransactionId("tx_admin_123");

        when(paymentMapper.toEntity(paymentRequestDto)).thenReturn(payment);
        when(externalPaymentApiService.processPayment(any(Payment.class))).thenReturn(apiResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        PaymentResponseDto result = paymentService.processPayment(paymentRequestDto, authenticatedId, roles);

        // Then
        assertThat(result).isNotNull();
        verify(paymentRepository).save(any(Payment.class));
        verify(externalPaymentApiService).processPayment(any(Payment.class));
    }

    @Test
    void getPaymentById_WithExistingPaymentAndAuthorizedUser_ShouldReturnPayment() {
        // Given
        String paymentId = "payment-123";
        String authenticatedId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String roles = "ROLE_USER";

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        PaymentResponseDto result = paymentService.getPaymentById(paymentId, authenticatedId, roles);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("payment-123");
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void getPaymentById_WithNonExistingPayment_ShouldThrowNotFoundException() {
        // Given
        String paymentId = "non-existing-payment";
        String authenticatedId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String roles = "ROLE_USER";

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId, authenticatedId, roles))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Payment with id=non-existing-payment not found");
    }

    @Test
    void getPaymentById_WithUnauthorizedUser_ShouldThrowAuthorizationException() {
        // Given
        String paymentId = "payment-123";
        String authenticatedId = "user-456";
        String roles = "ROLE_USER";

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentById(paymentId, authenticatedId, roles))
                .isInstanceOf(PaymentAuthorizationException.class)
                .hasMessage("You are not authorized to get this payment!");
    }

    @Test
    void getAllPaymentsByOrderId_WithAuthorizedUser_ShouldReturnPayments() {
        // Given
        String orderId = "68d6ccaa-bec0-4d1a-a328-f771c5b78d44";
        String authenticatedId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String roles = "ROLE_USER";
        List<Payment> payments = List.of(payment);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(payments);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        List<PaymentResponseDto> result = paymentService.getAllPaymentsByOrderId(orderId, authenticatedId, roles);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("payment-123");
        verify(paymentRepository).findByOrderId(orderId);
    }

    @Test
    void getAllPaymentsByOrderId_WithEmptyResult_ShouldReturnEmptyList() {
        // Given
        String orderId = "non-existing-order";
        String authenticatedId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String roles = "ROLE_USER";

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());

        // When
        List<PaymentResponseDto> result = paymentService.getAllPaymentsByOrderId(orderId, authenticatedId, roles);

        // Then
        assertThat(result).isEmpty();
        verify(paymentRepository).findByOrderId(orderId);
    }

    @Test
    void getAllPaymentsByOrderId_WithUnauthorizedUser_ShouldThrowAuthorizationException() {
        // Given
        String orderId = "68d6ccaa-bec0-4d1a-a328-f771c5b78d44";
        String authenticatedId = "user-456";
        String roles = "ROLE_USER";

        Payment otherUserPayment = new Payment();
        otherUserPayment.setUserId("user-999");

        List<Payment> payments = List.of(payment, otherUserPayment);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(payments);

        // When & Then
        assertThatThrownBy(() -> paymentService.getAllPaymentsByOrderId(orderId, authenticatedId, roles))
                .isInstanceOf(PaymentAuthorizationException.class)
                .hasMessage("You are not authorized to access these payments!");
    }

    @Test
    void getAllPaymentsByOrderId_WithAdminRole_ShouldReturnAllPayments() {
        // Given
        String orderId = "68d6ccaa-bec0-4d1a-a328-f771c5b78d44";
        String authenticatedId = "admin-user";
        String roles = "ROLE_ADMIN";
        List<Payment> payments = List.of(payment);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(payments);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        List<PaymentResponseDto> result = paymentService.getAllPaymentsByOrderId(orderId, authenticatedId, roles);

        // Then
        assertThat(result).hasSize(1);
        verify(paymentRepository).findByOrderId(orderId);
    }

    @Test
    void getAllPaymentsByUserId_WithAuthorizedUser_ShouldReturnPayments() {
        // Given
        String requestedUserId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String authenticatedId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String roles = "ROLE_USER";
        List<Payment> payments = List.of(payment);

        when(paymentRepository.findByUserId(requestedUserId)).thenReturn(payments);
        when(paymentMapper.toDto(payment)).thenReturn(paymentResponseDto);

        // When
        List<PaymentResponseDto> result = paymentService.getAllPaymentsByUserId(requestedUserId, authenticatedId, roles);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("payment-123");
        verify(paymentRepository).findByUserId(requestedUserId);
    }

    @Test
    void getAllPaymentsByUserId_WithUnauthorizedUser_ShouldThrowAuthorizationException() {
        // Given
        String requestedUserId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String authenticatedId = "user-456";
        String roles = "ROLE_USER";

        // When & Then
        assertThatThrownBy(() -> paymentService.getAllPaymentsByUserId(requestedUserId, authenticatedId, roles))
                .isInstanceOf(PaymentAuthorizationException.class)
                .hasMessage("You are not authorized to get this payment!");

        verify(paymentRepository, never()).findByUserId(anyString());
    }

    @Test
    void processOrderCreatedEvent_WithSuccessfulPayment_ShouldSaveAndSendEvent() {
        // Given
        ExternalPaymentApiResponse apiResponse = new ExternalPaymentApiResponse();
        apiResponse.setPaymentStatusNumber(2);
        apiResponse.setTransactionId("tx_event_123");

        when(externalPaymentApiService.processPayment(any(Payment.class))).thenReturn(apiResponse);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment paymentToSave = invocation.getArgument(0);
            paymentToSave.setId("b25c69da-00d6-442a-a922-a8c88ad34b62");
            return paymentToSave;
        });

        // When
        paymentService.processOrderCreatedEvent(orderCreatedEvent);

        // Then
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(savedPayment.getProcessorTransactionId()).isEqualTo("tx_event_123");
        assertThat(savedPayment.getOrderId()).isEqualTo(orderCreatedEvent.getOrderId().toString());
        assertThat(savedPayment.getUserId()).isEqualTo(orderCreatedEvent.getUserId().toString());
        assertThat(savedPayment.getPaymentAmount()).isEqualTo(orderCreatedEvent.getTotalAmount());
        assertThat(savedPayment.getCurrency()).isEqualTo(orderCreatedEvent.getCurrency());

        verify(kafkaTemplate).send(eq("payment-created-topic"), eventCaptor.capture());
        PaymentCreatedEvent sentEvent = eventCaptor.getValue();
        assertThat(sentEvent.getOrderId()).isEqualTo(orderCreatedEvent.getOrderId());
        assertThat(sentEvent.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(sentEvent.getPaymentId()).isEqualTo("b25c69da-00d6-442a-a922-a8c88ad34b62");
    }

    @Test
    void processOrderCreatedEvent_WithFailedPayment_ShouldSaveFailedPaymentAndSendEvent() {
        // Given
        ExternalPaymentApiResponse apiResponse = new ExternalPaymentApiResponse();
        apiResponse.setPaymentStatusNumber(3);
        apiResponse.setTransactionId("tx_event_failed_123");

        when(externalPaymentApiService.processPayment(any(Payment.class))).thenReturn(apiResponse);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment paymentToSave = invocation.getArgument(0);
            paymentToSave.setId("payment-123");
            return paymentToSave;
        });

        // When
        paymentService.processOrderCreatedEvent(orderCreatedEvent);

        // Then
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getErrorMessage()).isEqualTo("Payment processing failed");
        assertThat(savedPayment.getProcessorTransactionId()).isEqualTo("tx_event_failed_123");

        verify(kafkaTemplate).send(eq("payment-created-topic"), eventCaptor.capture());
        PaymentCreatedEvent sentEvent = eventCaptor.getValue();
        assertThat(sentEvent.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(sentEvent.getErrorMessage()).isEqualTo("Payment processing failed");
    }

    @Test
    void processOrderCreatedEvent_WithExternalApiException_ShouldSaveFailedPayment() {
        // Given
        RuntimeException apiException = new RuntimeException("API Error");

        when(externalPaymentApiService.processPayment(any(Payment.class))).thenThrow(apiException);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // When
        paymentService.processOrderCreatedEvent(orderCreatedEvent);

        // Then
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(savedPayment.getErrorMessage()).isEqualTo("API Error");
        assertThat(savedPayment.getErrorCode()).isEqualTo("PAYMENT_PROCESSING_ERROR");

        verify(kafkaTemplate).send(anyString(), any(PaymentCreatedEvent.class));
    }

    @Test
    void processOrderCreatedEvent_WithKafkaException_ShouldLogErrorButNotFail() {
        // Given
        ExternalPaymentApiResponse apiResponse = new ExternalPaymentApiResponse();
        apiResponse.setPaymentStatusNumber(2);
        apiResponse.setTransactionId("tx_kafka_error_123");

        when(externalPaymentApiService.processPayment(any(Payment.class))).thenReturn(apiResponse);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        doThrow(new RuntimeException("Kafka error")).when(kafkaTemplate).send(anyString(), any(PaymentCreatedEvent.class));

        // When
        paymentService.processOrderCreatedEvent(orderCreatedEvent);

        // Then
        verify(paymentRepository).save(any(Payment.class));
        verify(kafkaTemplate).send(anyString(), any(PaymentCreatedEvent.class));
    }

    @Test
    void authorization_WithNullRoles_ShouldThrowAuthorizationException() {
        // Given
        String authenticatedId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String roles = null;

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(paymentRequestDto, authenticatedId, roles))
                .isInstanceOf(PaymentAuthorizationException.class)
                .hasMessage("You are not authorized to perform this payment!");
    }

    @Test
    void authorization_WithEmptyRoles_ShouldThrowAuthorizationException() {
        // Given
        String authenticatedId = "b25c69da-00d6-442a-a922-a8c88ad34b62";
        String roles = "";

        // When & Then
        assertThatThrownBy(() -> paymentService.processPayment(paymentRequestDto, authenticatedId, roles))
                .isInstanceOf(PaymentAuthorizationException.class)
                .hasMessage("You are not authorized to perform this payment!");
    }
}
