package com.example.coffee.domain.product.popular;

import com.example.coffee.domain.auth.dto.request.SignupRequest;
import com.example.coffee.domain.auth.service.AuthService;
import com.example.coffee.domain.order.dto.request.OrderCreateRequest;
import com.example.coffee.domain.order.dto.request.OrderItemRequest;
import com.example.coffee.domain.order.service.OrderService;
import com.example.coffee.domain.payment.service.PaymentService;
import com.example.coffee.domain.product.popular.repository.PopularMenuRedisRepository;
import com.example.coffee.domain.product.popular.service.PopularMenuAggregator;
import com.example.coffee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = "outbox.relay.initial-delay-ms=3600000")
class PopularMenuAggregationTest {

    @Autowired OrderService orderService;
    @Autowired AuthService authService;
    @Autowired PaymentService paymentService;
    @Autowired UserRepository userRepository;
    @Autowired PopularMenuAggregator aggregator;
    @Autowired PopularMenuRedisRepository popularMenuRedisRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void 주문하면_주문일_키에_판매_수량이_반영되고_취소하면_빠진다() {
        // given
        Long userId = 포인트_충전된_사용자_생성(50_000);
        Long productId = 상품_생성(4_500, 100);
        LocalDate today = LocalDate.now();

        // when: 3잔 주문
        Long orderId = orderService.createOrder(userId,
                new OrderCreateRequest(List.of(new OrderItemRequest(productId, 3)))).order().orderId();

        // then
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(popularMenuRedisRepository.getScore(today, productId)).isEqualTo(3.0));

        // when: 취소
        orderService.cancelOrder(userId, orderId);

        // then
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(popularMenuRedisRepository.getScore(today, productId)).isEqualTo(0.0));
    }

    @Test
    void 같은_이벤트를_두_번_받아도_한_번만_반영된다() {
        Long productId = 테스트용_상품_ID();
        String payload = completedPayload(UUID.randomUUID().toString(), productId, 2, LocalDateTime.now());

        boolean first = aggregator.aggregate(payload);
        boolean second = aggregator.aggregate(payload);

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(popularMenuRedisRepository.getScore(LocalDate.now(), productId)).isEqualTo(2.0);
    }

    @Test
    void 같은_이벤트가_동시에_도착해도_한_번만_반영된다() throws Exception {
        Long productId = 테스트용_상품_ID();
        String payload = completedPayload(UUID.randomUUID().toString(), productId, 1, LocalDateTime.now());

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return aggregator.aggregate(payload);
            }));
        }
        ready.await();
        start.countDown();

        int appliedCount = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                appliedCount++;
            }
        }
        executor.shutdown();

        assertThat(appliedCount).isEqualTo(1);
        assertThat(popularMenuRedisRepository.getScore(LocalDate.now(), productId)).isEqualTo(1.0);
    }

    @Test
    void 어제_주문을_오늘_취소하면_어제_키에서_빠진다() {
        Long productId = 테스트용_상품_ID();
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        aggregator.aggregate(completedPayload(UUID.randomUUID().toString(), productId, 2, yesterday));
        aggregator.aggregate(canceledPayload(UUID.randomUUID().toString(), productId, 2, yesterday));

        assertThat(popularMenuRedisRepository.getScore(yesterday.toLocalDate(), productId)).isEqualTo(0.0);
        assertThat(popularMenuRedisRepository.getScore(LocalDate.now(), productId)).isEqualTo(0.0);
    }

    @Test
    void 집계_기간이_지난_주문은_반영하지_않는다() {
        Long productId = 테스트용_상품_ID();
        LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(10);

        boolean applied = aggregator.aggregate(
                completedPayload(UUID.randomUUID().toString(), productId, 5, tenDaysAgo));

        assertThat(applied).isFalse();
        assertThat(popularMenuRedisRepository.getScore(tenDaysAgo.toLocalDate(), productId)).isEqualTo(0.0);
    }

    // ===== 메시지 만들기 =====

    private String completedPayload(String eventId, Long productId, int quantity, LocalDateTime orderedAt) {
        return payload(eventId, "ORDER_COMPLETED", productId, quantity, orderedAt);
    }

    private String canceledPayload(String eventId, Long productId, int quantity, LocalDateTime orderedAt) {
        return payload(eventId, "ORDER_CANCELED", productId, quantity, orderedAt);
    }

    private String payload(String eventId, String eventType, Long productId, int quantity, LocalDateTime orderedAt) {
        return """
                {"eventId":"%s","eventType":"%s","orderId":1,"userId":1,
                 "items":[{"productId":%d,"quantity":%d,"unitPrice":4500,"amount":%d}],
                 "totalAmount":%d,"orderedAt":"%s","occurredAt":"%s"}
                """.formatted(eventId, eventType, productId, quantity, quantity * 4500,
                quantity * 4500, orderedAt, orderedAt);
    }

    /** 다른 테스트나 개발 데이터와 점수가 섞이지 않도록 큰 범위의 임의 상품 ID 사용 */
    private Long 테스트용_상품_ID() {
        return ThreadLocalRandom.current().nextLong(1_000_000L, 9_000_000L);
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