package com.example.tasks.paymentservice.integration;

import com.example.tasks.paymentservice.config.SecurityConfig;
import com.example.tasks.paymentservice.controller.PaymentController;
import com.example.tasks.paymentservice.dto.PaymentRequestDto;
import com.example.tasks.paymentservice.dto.PaymentResponseDto;
import com.example.tasks.paymentservice.exception.PaymentAuthorizationException;
import com.example.tasks.paymentservice.exception.PaymentNotFoundException;
import com.example.tasks.paymentservice.security.InternalAuthFilter;
import com.example.tasks.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.tasks.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PaymentController.class,
        excludeAutoConfiguration = SecurityConfig.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = InternalAuthFilter.class))
@ActiveProfiles("test")
public class PaymentControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    private final String TEST_USER_ID = "123e4567-e89b-12d3-a456-426614174000";
    private final String TEST_USER_ID_2 = "123e4567-e89b-12d3-a456-426614174001";
    private final String TEST_ORDER_ID = "223e4567-e89b-12d3-a456-426614174000";
    private final String TEST_PAYMENT_ID = "payment-123";
    private final String USER_ROLES = "ROLE_USER";
    private final String ADMIN_ROLES = "ROLE_ADMIN,ROLE_USER";

    @Test
    void processPayment_WithValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        PaymentRequestDto request = createPaymentRequest();
        PaymentResponseDto response = createPaymentResponse();

        when(paymentService.processPayment(any(PaymentRequestDto.class), eq(TEST_USER_ID), eq(USER_ROLES)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/payment")
                        .header("X-User-ID", TEST_USER_ID)
                        .header("X-User-Roles", USER_ROLES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TEST_PAYMENT_ID))
                .andExpect(jsonPath("$.orderId").value(TEST_ORDER_ID))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void processPayment_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        PaymentRequestDto request = new PaymentRequestDto(); // Invalid - missing required fields

        // When & Then
        mockMvc.perform(post("/payment")
                        .header("X-User-ID", TEST_USER_ID)
                        .header("X-User-Roles", USER_ROLES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processPayment_WithAuthorizationException_ShouldReturnForbidden() throws Exception {
        // Given
        PaymentRequestDto request = createPaymentRequest();

        when(paymentService.processPayment(any(PaymentRequestDto.class), eq(TEST_USER_ID_2), eq(USER_ROLES)))
                .thenThrow(new PaymentAuthorizationException("Not authorized"));

        // When & Then
        mockMvc.perform(post("/payment")
                        .header("X-User-ID", TEST_USER_ID_2)
                        .header("X-User-Roles", USER_ROLES)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPaymentById_WithExistingPayment_ShouldReturnPayment() throws Exception {
        // Given
        PaymentResponseDto response = createPaymentResponse();

        when(paymentService.getPaymentById(TEST_PAYMENT_ID, TEST_USER_ID, USER_ROLES))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/payment/{paymentId}", TEST_PAYMENT_ID)
                        .header("X-User-ID", TEST_USER_ID)
                        .header("X-User-Roles", USER_ROLES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_PAYMENT_ID))
                .andExpect(jsonPath("$.orderId").value(TEST_ORDER_ID));
    }

    @Test
    void getPaymentById_WithNonExistingPayment_ShouldReturnNotFound() throws Exception {
        // Given
        when(paymentService.getPaymentById("non-existing", TEST_USER_ID, USER_ROLES))
                .thenThrow(new PaymentNotFoundException("Payment not found"));

        // When & Then
        mockMvc.perform(get("/payment/{paymentId}", "non-existing")
                        .header("X-User-ID", TEST_USER_ID)
                        .header("X-User-Roles", USER_ROLES))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentsByOrderId_WithAuthorizedUser_ShouldReturnPayments() throws Exception {
        // Given
        List<PaymentResponseDto> responses = List.of(
                createPaymentResponse(),
                createPaymentResponse()
        );

        when(paymentService.getAllPaymentsByOrderId(TEST_ORDER_ID, TEST_USER_ID, USER_ROLES))
                .thenReturn(responses);

        // When & Then
        mockMvc.perform(get("/payment/order/{orderId}", TEST_ORDER_ID)
                        .header("X-User-ID", TEST_USER_ID)
                        .header("X-User-Roles", USER_ROLES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderId").value(TEST_ORDER_ID))
                .andExpect(jsonPath("$[1].orderId").value(TEST_ORDER_ID));
    }

    @Test
    void getPaymentsByOrderId_WithUnauthorizedUser_ShouldReturnForbidden() throws Exception {
        // Given
        when(paymentService.getAllPaymentsByOrderId(TEST_ORDER_ID, TEST_USER_ID_2, USER_ROLES))
                .thenThrow(new PaymentAuthorizationException("Not authorized"));

        // When & Then
        mockMvc.perform(get("/payment/order/{orderId}", TEST_ORDER_ID)
                        .header("X-User-ID", TEST_USER_ID_2)
                        .header("X-User-Roles", USER_ROLES))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPaymentsByUserId_WithAdminRole_ShouldReturnPayments() throws Exception {
        // Given
        List<PaymentResponseDto> responses = List.of(
                createPaymentResponse(),
                createPaymentResponse()
        );

        when(paymentService.getAllPaymentsByUserId(TEST_USER_ID, TEST_USER_ID, ADMIN_ROLES))
                .thenReturn(responses);

        // When & Then
        mockMvc.perform(get("/payment/user/{userId}", TEST_USER_ID)
                        .header("X-User-ID", TEST_USER_ID)
                        .header("X-User-Roles", ADMIN_ROLES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(TEST_USER_ID));
    }

    @Test
    void getPaymentsByUserId_WithSameUser_ShouldReturnPayments() throws Exception {
        // Given
        List<PaymentResponseDto> responses = List.of(createPaymentResponse());

        when(paymentService.getAllPaymentsByUserId(TEST_USER_ID, TEST_USER_ID, USER_ROLES))
                .thenReturn(responses);

        // When & Then
        mockMvc.perform(get("/payment/user/{userId}", TEST_USER_ID)
                        .header("X-User-ID", TEST_USER_ID)
                        .header("X-User-Roles", USER_ROLES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(TEST_USER_ID));
    }

    @Test
    void getPaymentsByUserId_WithDifferentUser_ShouldReturnForbidden() throws Exception {
        // Given
        when(paymentService.getAllPaymentsByUserId(TEST_USER_ID, TEST_USER_ID_2, USER_ROLES))
                .thenThrow(new PaymentAuthorizationException("Not authorized"));

        // When & Then
        mockMvc.perform(get("/payment/user/{userId}", TEST_USER_ID)
                        .header("X-User-ID", TEST_USER_ID_2)
                        .header("X-User-Roles", USER_ROLES))
                .andExpect(status().isForbidden());
    }

    // Helper methods
    private PaymentRequestDto createPaymentRequest() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setPaymentMethodToken("pm_token_123");
        request.setAmount(new BigDecimal("100.50"));
        request.setCurrency("USD");
        request.setOrderId(TEST_ORDER_ID);
        request.setUserId(TEST_USER_ID);
        request.setDescription("Test payment");
        return request;
    }

    private PaymentResponseDto createPaymentResponse() {
        PaymentResponseDto response = new PaymentResponseDto();
        response.setId(TEST_PAYMENT_ID);
        response.setOrderId(TEST_ORDER_ID);
        response.setUserId(TEST_USER_ID);
        response.setStatus(PaymentStatus.SUCCESS);
        response.setTimestamp(LocalDateTime.now());
        response.setAmount(new BigDecimal("100.50"));
        response.setCurrency("USD");
        response.setDescription("Test payment");
        response.setProcessorTransactionId("txn_123");
        return response;
    }
}
