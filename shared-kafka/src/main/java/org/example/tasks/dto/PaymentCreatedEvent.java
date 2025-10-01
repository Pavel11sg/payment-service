package org.example.tasks.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.tasks.model.PaymentStatus;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreatedEvent {
    private UUID orderId;
    private PaymentStatus status;
    private String paymentId;
    private String errorMessage;
}
