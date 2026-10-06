package com.example.coffee.domain.dataplatform;

import com.example.coffee.domain.auth.dto.request.SignupRequest;
import com.example.coffee.domain.auth.service.AuthService;
import com.example.coffee.domain.dataplatform.entity.CollectedOrderEvent;
import com.example.coffee.domain.dataplatform.repository.CollectedOrderEventRepository;
import com.example.coffee.domain.dataplatform.service.DataPlatformCollector;
import com.example.coffee.domain.order.dto.request.OrderCreateRequest;
import com.example.coffee.domain.order.dto.request.OrderItemRequest;
import com.example.coffee.domain.order.service.OrderService;
import com.example.coffee.domain.payment.service.PaymentService;
import com.example.coffee.domain.user.repository.UserRepository;
import com.example.coffee.global.outbox.OutboxEvent;
import com.example.coffee.global.outbox.OutboxEventRepository;
import com.example.coffee.global.outbox.OutboxPublisher;
import com.example.coffee.global.outbox.OutboxStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = "outbox.relay.initial-delay-ms=3600000")   // 스케줄러는 테스트 중 실행되지 않게
class DataPlatformConsumerTest {

    @Autowired OrderService orderService;
    @Autowired AuthService authService;
    @Autowired PaymentService paymentService;
    @Autowired UserRepository userRepository;
    @Autowired OutboxEventRepository outboxEventRepository;
    @Autowired OutboxPublisher outboxPublisher;
    @Autowired CollectedOrderEventRepository collectedOrderEventRepository;
    @Autowired DataPlatformCollector collector;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void 주문하면_데이터_플랫폼에_사용자_메뉴_결제금액이_적재된다() {
        // given
        Long userId = 포인트_충전된_사용자_생성(20_000);
        Long productId = 상품_생성(4_500, 10);

        // when
        Long orderId = orderService.createOrder(userId,
                new OrderCreateRequest(List.of(new OrderItemRequest(productId, 2)))).order().orderId();

        // then: 주문 → Outbox → Kafka → 컨슈머 → 적재까지 비동기
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(collectedOrderEventRepository.findAllByOrderIdOrderByIdAsc(orderId)).hasSize(1));

        CollectedOrderEvent collected = collectedOrderEventRepository.findAllByOrderIdOrderByIdAsc(orderId).get(0);
        assertThat(collected.getEventType()).isEqualTo("ORDER_COMPLETED");
        assertThat(collected.getUserId()).isEqualTo(userId);
        assertThat(collected.getTotalAmount()).isEqualTo(9_000L);
        assertThat(collected.getPayload()).contains("\"productId\":" + productId);
    }

    @Test
    void 주문_후_취소하면_완료와_취소_이벤트가_순서대로_적재된다() {
        Long userId = 포인트_충전된_사용자_생성(20_000);
        Long productId = 상품_생성(4_500, 10);
        Long orderId = orderService.createOrder(userId,
                new OrderCreateRequest(List.of(new OrderItemRequest(productId, 1)))).order().orderId();

        orderService.cancelOrder(userId, orderId);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(collectedOrderEventRepository.findAllByOrderIdOrderByIdAsc(orderId))
                        .extracting(CollectedOrderEvent::getEventType)
                        .containsExactly("ORDER_COMPLETED", "ORDER_CANCELED"));
    }

    @Test
    void 같은_이벤트가_다시_발행되어도_한_번만_적재된다() {
        // given: 주문 → 적재 완료까지 대기
        Long userId = 포인트_충전된_사용자_생성(20_000);
        Long productId = 상품_생성(4_500, 10);
        Long orderId = orderService.createOrder(userId,
                new OrderCreateRequest(List.of(new OrderItemRequest(productId, 1)))).order().orderId();

        OutboxEvent outbox = outboxEventRepository.findAll().stream()
                .filter(e -> e.getMessageKey().equals(String.valueOf(orderId)))
                .findFirst().orElseThrow();

        await().atMost(Duration.ofSeconds(20)).until(() ->
                collectedOrderEventRepository.existsByEventId(outbox.getEventId()));

        // when: "PUBLISHED 기록 전에 서버가 죽은" 상황을 흉내 → 다시 PENDING으로 돌려 Relay가 재발행
        jdbcTemplate.update("UPDATE outbox_events SET status = ?, created_at = ? WHERE id = ?",
                OutboxStatus.PENDING.name(), LocalDateTime.now().minusMinutes(1), outbox.getId());
        outboxPublisher.relayPending(50);

        // then: 메시지는 두 번 갔지만, 몇 초 동안 지켜봐도 적재는 1건 그대로
        await().during(Duration.ofSeconds(5)).atMost(Duration.ofSeconds(8)).until(() ->
                collectedOrderEventRepository.countByEventId(outbox.getEventId()) == 1);
    }

    @Test
    void 같은_메시지를_직접_두_번_넣어도_한_번만_적재된다() {
        String eventId = UUID.randomUUID().toString();
        String payload = """
                {"eventId":"%s","eventType":"ORDER_COMPLETED","orderId":999999,"userId":1,
                 "items":[{"productId":1,"quantity":1,"unitPrice":4500,"amount":4500}],
                 "totalAmount":4500,"orderedAt":"2026-10-06T10:00:00","occurredAt":"2026-10-06T10:00:00"}
                """.formatted(eventId);

        boolean first = collector.collect(payload);
        boolean second = collector.collect(payload);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(collectedOrderEventRepository.countByEventId(eventId)).isEqualTo(1);
    }

    // ===== 테스트 데이터 준비 =====

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