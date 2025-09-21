package com.example.tasks.paymentservice.controller;

import com.example.tasks.paymentservice.dto.PaymentRequestDto;
import com.example.tasks.paymentservice.dto.PaymentResponseDto;
import com.example.tasks.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payment")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDto> processPayment(
            @Valid @RequestBody PaymentRequestDto paymentRequest,
            @RequestHeader(name = "X-User-ID") String userId,
            @RequestHeader(name = "X-User-Roles") String roles
    ) {
        PaymentResponseDto responseDto = paymentService.processPayment(paymentRequest, userId, roles);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPaymentById(
            @PathVariable(name = "paymentId") String paymentId,
            @RequestHeader(name = "X-User-ID") String userId,
            @RequestHeader(name = "X-User-Roles") String roles
    ) {
        PaymentResponseDto responseDto = paymentService.getPaymentById(paymentId, userId, roles);
        return ResponseEntity.status(HttpStatus.OK).body(responseDto);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByOrderId(
            @PathVariable(name = "orderId") String orderId,
            @RequestHeader(name = "X-User-ID") String userId,
            @RequestHeader(name = "X-User-Roles") String roles
    ) {
        List<PaymentResponseDto> payments = paymentService.getAllPaymentsByOrderId(orderId, userId, roles);
        return ResponseEntity.status(HttpStatus.OK).body(payments);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByUserId(
            @PathVariable(name = "userId") String requestedUserId,
            @RequestHeader(name = "X-User-ID") String authenticatedUserId,
            @RequestHeader(name = "X-User-Roles") String roles
    ) {
        List<PaymentResponseDto> payments = paymentService.getAllPaymentsByUserId(requestedUserId, authenticatedUserId, roles);
        return ResponseEntity.status(HttpStatus.OK).body(payments);
    }
}
