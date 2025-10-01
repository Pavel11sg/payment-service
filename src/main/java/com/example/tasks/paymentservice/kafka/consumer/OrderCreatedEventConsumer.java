package com.example.tasks.paymentservice.kafka.consumer;



import com.example.tasks.paymentservice.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.example.tasks.dto.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCreatedEventConsumer {

    private final PaymentService paymentService;

    public OrderCreatedEventConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = "${kafka.topics.order-created}",
            groupId = "payment-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderCreatedEvent(OrderCreatedEvent orderCreatedEvent) {
        log.info("Received OrderCreatedEvent for order: {}", orderCreatedEvent.getOrderId());
        try {
            paymentService.processOrderCreatedEvent(orderCreatedEvent);
        } catch (Exception e) {
            log.error("Error handling OrderCreatedEvent for order: {}",
                    orderCreatedEvent.getOrderId(), e);
        }
    }
}
