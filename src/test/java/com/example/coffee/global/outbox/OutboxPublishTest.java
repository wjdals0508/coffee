package com.example.coffee.global.outbox;

import com.example.coffee.domain.auth.dto.request.SignupRequest;
import com.example.coffee.domain.auth.service.AuthService;
import com.example.coffee.domain.order.dto.request.OrderCreateRequest;
import com.example.coffee.domain.order.dto.request.OrderItemRequest;
import com.example.coffee.domain.order.service.OrderService;
import com.example.coffee.domain.payment.service.PaymentService;
import com.example.coffee.domain.user.repository.UserRepository;
import com.example.coffee.global.config.kafka.KafkaTopicConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = "outbox.relay.initial-delay-ms=3600000")   // 스케줄러는 테스트 중 실행되지 않게
class OutboxPublishTest {

    @Autowired OrderService orderService;
    @Autowired AuthService authService;
    @Autowired PaymentService paymentService;
    @Autowired UserRepository userRepository;
    @Autowired OutboxEventRepository outboxEventRepository;
    @Autowired OutboxPublisher outboxPublisher;
    @Autowired JdbcTemplate jdbcTemplate;

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Test
    void 주문이_커밋되면_즉시_Kafka로_발행되고_PUBLISHED가_된다() {
        // given
        Long userId = 포인트_충전된_사용자_생성(10_000);
        Long productId = 상품_생성(4_500, 10);

        // when
        Long orderId = orderService.createOrder(userId,
                new OrderCreateRequest(List.of(new OrderItemRequest(productId, 1)))).order().orderId();

        // then: 비동기 발행이 끝날 때까지 최대 15초 대기
        OutboxEvent event = findByMessageKey(String.valueOf(orderId));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(outboxEventRepository.findById(event.getId()).orElseThrow().getStatus())
                        .isEqualTo(OutboxStatus.PUBLISHED));

        // Kafka 토픽에 같은 eventId의 메시지가 실제로 있는지 확인
        ConsumerRecord<String, String> record = consumeByEventId(event.getEventId(), Duration.ofSeconds(15))
                .orElseThrow(() -> new AssertionError("토픽에서 메시지를 찾지 못했습니다."));
        assertThat(record.key()).isEqualTo(String.valueOf(orderId));
        assertThat(record.value()).isEqualTo(event.getPayload());
        assertThat(headerValue(record, OutboxPublisher.HEADER_EVENT_TYPE)).isEqualTo("ORDER_COMPLETED");
    }

    @Test
    void 밀린_PENDING_이벤트는_재발행_Relay가_발행한다() {
        // given: 즉시 발행이 실패해 남은 상황을 흉내 — 직접 PENDING 이벤트를 만들고 생성 시각을 과거로
        String eventId = UUID.randomUUID().toString();
        OutboxEvent pending = outboxEventRepository.save(OutboxEvent.create(
                eventId, KafkaTopicConfig.ORDER_EVENTS_TOPIC, "relay-test",
                "ORDER_COMPLETED", "{\"eventId\":\"" + eventId + "\"}"));
        jdbcTemplate.update("UPDATE outbox_events SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusMinutes(1), pending.getId());

        // when
        int processed = outboxPublisher.relayPending(50);

        // then
        assertThat(processed).isGreaterThanOrEqualTo(1);
        OutboxEvent relayed = outboxEventRepository.findById(pending.getId()).orElseThrow();
        assertThat(relayed.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(relayed.getPublishedAt()).isNotNull();
        assertThat(consumeByEventId(eventId, Duration.ofSeconds(15))).isPresent();
    }

    @Test
    void 생성된_지_얼마_안_된_PENDING_이벤트는_Relay가_건드리지_않는다() {
        // given: 방금 만들어진 PENDING (즉시 발행이 진행 중일 수 있는 이벤트)
        String eventId = UUID.randomUUID().toString();
        OutboxEvent fresh = outboxEventRepository.save(OutboxEvent.create(
                eventId, KafkaTopicConfig.ORDER_EVENTS_TOPIC, "fresh-test",
                "ORDER_COMPLETED", "{\"eventId\":\"" + eventId + "\"}"));

        // when
        outboxPublisher.relayPending(50);

        // then
        assertThat(outboxEventRepository.findById(fresh.getId()).orElseThrow().getStatus())
                .isEqualTo(OutboxStatus.PENDING);
    }

    // ===== 헬퍼 =====

    private OutboxEvent findByMessageKey(String messageKey) {
        return outboxEventRepository.findAll().stream()
                .filter(e -> e.getMessageKey().equals(messageKey))
                .findFirst()
                .orElseThrow();
    }

    private Optional<ConsumerRecord<String, String>> consumeByEventId(String eventId, Duration timeout) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class))) {

            consumer.subscribe(List.of(KafkaTopicConfig.ORDER_EVENTS_TOPIC));
            long deadline = System.currentTimeMillis() + timeout.toMillis();

            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (eventId.equals(headerValue(record, OutboxPublisher.HEADER_EVENT_ID))) {
                        return Optional.of(record);
                    }
                }
            }
            return Optional.empty();
        }
    }

    private String headerValue(ConsumerRecord<String, String> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private Long 포인트_충전된_사용자_생성(long amount) {
        String email = "test-" + UUID.randomUUID() + "@coffee.com";
        authService.signup(new SignupRequest("테스터", email, "password123"));
        Long userId = userRepository.findByEmail(email).orElseThrow().getId();
        paymentService.chargePoint(userId, amount);
        return userId;
    }

    private Long 상품_생성(long price, int stock) {
        Number categoryId = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("categories")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(Map.of("name", "테스트카테고리-" + UUID.randomUUID()));

        Number productId = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("products")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(Map.of(
                        "category_id", categoryId.longValue(),
                        "name", "테스트상품-" + UUID.randomUUID(),
                        "price", price,
                        "description", "테스트용 상품",
                        "stock", stock,
                        "status", "ON_SALE"));
        return productId.longValue();
    }
}