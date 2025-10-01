package org.example.tasks.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private UUID orderId;
    private UUID userId;
    private BigDecimal totalAmount;
    private String currency;
    private Instant timestamp;
    private String paymentMethodToken;
}
