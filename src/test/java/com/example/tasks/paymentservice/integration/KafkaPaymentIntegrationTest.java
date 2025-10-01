package com.example.tasks.paymentservice.integration;

import com.example.tasks.paymentservice.dto.ExternalPaymentApiResponse;
import com.example.tasks.paymentservice.model.Payment;
import com.example.tasks.paymentservice.repository.PaymentRepository;
import com.example.tasks.paymentservice.service.ExternalPaymentApiService;
import org.example.tasks.dto.OrderCreatedEvent;
import org.example.tasks.dto.PaymentCreatedEvent;
import org.example.tasks.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"order-created-topic-test", "payment-created-topic-test"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:0",
                "port=0"
        }
)
@Testcontainers
@ActiveProfiles("test")
@DirtiesContext
class KafkaPaymentIntegrationTest {
    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestKafkaConsumer testKafkaConsumer;
    @MockitoBean
    private ExternalPaymentApiService externalPaymentApiService;

    @BeforeEach
    void setUp() throws InterruptedException {
        paymentRepository.deleteAll();
        testKafkaConsumer.clear();

        ExternalPaymentApiResponse successResponse = new ExternalPaymentApiResponse();
        successResponse.setTransactionId("txn_test_" + UUID.randomUUID());
        successResponse.setPaymentStatusNumber(10);

        when(externalPaymentApiService.processPayment(any(Payment.class)))
                .thenReturn(successResponse);
        Thread.sleep(2000);
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

        System.out.println("=== TEST START ===");
        System.out.println("Sending OrderCreatedEvent for order: " + orderEvent.getOrderId());
        System.out.println("Test consumer group: " + testKafkaConsumer.getConsumerGroupId());

        // When
        kafkaTemplate.send("order-created-topic-test", orderEvent);
        kafkaTemplate.flush();
        System.out.println("OrderCreatedEvent sent");

        // Then
        PaymentCreatedEvent paymentEvent = null;
        for (int i = 0; i < 100; i++) {
            paymentEvent = testKafkaConsumer.getPaymentCreatedEvents().poll();
            if (paymentEvent != null) {
                System.out.println("PaymentCreatedEvent received: " + paymentEvent.getPaymentId());
                break;
            }
            Thread.sleep(100);
            if (i % 10 == 0) {
                System.out.println("Waiting for PaymentCreatedEvent... " + i);
            }
        }

        System.out.println("Final paymentEvent: " + paymentEvent);
        assertThat(paymentEvent).isNotNull();
        assertThat(paymentEvent.getOrderId()).isEqualTo(orderEvent.getOrderId());
        assertThat(paymentEvent.getPaymentId()).isNotNull();
        assertThat(paymentEvent.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
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
        private final String consumerGroupId = "test-consumer-group-" + UUID.randomUUID();

        @org.springframework.kafka.annotation.KafkaListener(
                topics = "payment-created-topic-test",
                groupId = "#{__listener.consumerGroupId}"
        )
        public void handlePaymentCreatedEvent(PaymentCreatedEvent event) {
            paymentCreatedEvents.offer(event);
            System.out.println("Received PaymentCreatedEvent: " + event.getPaymentId() + " with status: " + event.getStatus());
        }

        public String getConsumerGroupId() {
            return consumerGroupId;
        }

        public BlockingQueue<PaymentCreatedEvent> getPaymentCreatedEvents() {
            return paymentCreatedEvents;
        }

        public void clear() {
            paymentCreatedEvents.clear();
        }
    }
}
