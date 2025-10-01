package com.example.tasks.paymentservice.integration;

import com.example.tasks.paymentservice.TestContainerConfig;
import com.example.tasks.paymentservice.repository.PaymentRepository;
import org.example.tasks.dto.OrderCreatedEvent;
import org.example.tasks.dto.PaymentCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"order-created-topic-test", "payment-created-topic-test"})
@TestPropertySource(properties = {
        "kafka.topics.order-created=order-created-topic-test",
        "kafka.topics.payment-created=payment-created-topic-test"
})
@Testcontainers
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
class KafkaPaymentIntegrationTest {
    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestKafkaConsumer testKafkaConsumer;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        testKafkaConsumer.clear();
    }

    @Test
    void processOrderCreatedEvent_ShouldProcessPaymentAndSendPaymentCreatedEvent() throws Exception {
        // Given
        OrderCreatedEvent orderEvent = new OrderCreatedEvent();
        orderEvent.setOrderId(UUID.randomUUID());
        orderEvent.setUserId(UUID.randomUUID());
        orderEvent.setTotalAmount(new BigDecimal("150.75"));
        orderEvent.setCurrency("USD");
        orderEvent.setTimestamp(Instant.now());
        orderEvent.setPaymentMethodToken("pm_token_123");

        // When
        kafkaTemplate.send("order-created-topic-test", orderEvent);

        // Then
        PaymentCreatedEvent paymentEvent = null;
        for (int i = 0; i < 10 && paymentEvent == null; i++) {
            paymentEvent = testKafkaConsumer.getPaymentCreatedEvents().poll();
            Thread.sleep(1000);
        }

        assertThat(paymentEvent).isNotNull();
        assertThat(paymentEvent.getOrderId()).isEqualTo(orderEvent.getOrderId());
        assertThat(paymentEvent.getPaymentId()).isNotNull();

        assertThat(paymentRepository.findByOrderId(orderEvent.getOrderId().toString())).hasSize(1);
    }

    @TestConfiguration
    @EnableKafka
    static class TestConfig {

        @Bean
        public TestKafkaConsumer testKafkaConsumer() {
            return new TestKafkaConsumer();
        }
    }

    static class TestKafkaConsumer {
        private final BlockingQueue<PaymentCreatedEvent> paymentCreatedEvents = new LinkedBlockingQueue<>();

        @org.springframework.kafka.annotation.KafkaListener(
                topics = "payment-created-topic-test",
                groupId = "test-consumer-group"
        )
        public void handlePaymentCreatedEvent(PaymentCreatedEvent event) {
            paymentCreatedEvents.offer(event);
        }

        public BlockingQueue<PaymentCreatedEvent> getPaymentCreatedEvents() {
            return paymentCreatedEvents;
        }

        public void clear() {
            paymentCreatedEvents.clear();
        }
    }
}
