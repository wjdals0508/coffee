package com.example.coffee.domain.order;

import com.example.coffee.domain.auth.dto.request.SignupRequest;
import com.example.coffee.domain.auth.service.AuthService;
import com.example.coffee.domain.order.dto.request.OrderCreateRequest;
import com.example.coffee.domain.order.dto.request.OrderItemRequest;
import com.example.coffee.domain.order.dto.response.OrderCreateResponse;
import com.example.coffee.domain.order.event.OrderEventPayload;
import com.example.coffee.domain.order.event.OrderEventType;
import com.example.coffee.domain.order.service.OrderService;
import com.example.coffee.domain.payment.service.PaymentService;
import com.example.coffee.domain.user.repository.UserRepository;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import com.example.coffee.global.outbox.OutboxEvent;
import com.example.coffee.global.outbox.OutboxEventRepository;
import com.example.coffee.global.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class OrderEventServiceTest {

    @Autowired OrderService orderService;
    @Autowired AuthService authService;
    @Autowired PaymentService paymentService;
    @Autowired UserRepository userRepository;
    @Autowired OutboxEventRepository outboxEventRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void 주문하면_같은_트랜잭션에서_주문_완료_이벤트가_저장된다() {
        // given
        Long userId = 포인트_충전된_사용자_생성(10_000);
        Long productId = 상품_생성(4_500, 10);

        // when
        OrderCreateResponse response = orderService.createOrder(userId,
                new OrderCreateRequest(List.of(new OrderItemRequest(productId, 2))));

        // then
        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getEventType()).isEqualTo(OrderEventType.ORDER_COMPLETED.name());
        assertThat(event.getMessageKey()).isEqualTo(String.valueOf(response.order().orderId()));

        OrderEventPayload payload = objectMapper.readValue(event.getPayload(), OrderEventPayload.class);
        assertThat(payload.eventId()).isEqualTo(event.getEventId());
        assertThat(payload.orderId()).isEqualTo(response.order().orderId());
        assertThat(payload.userId()).isEqualTo(userId);
        assertThat(payload.totalAmount()).isEqualTo(9_000L);
        assertThat(payload.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.productId()).isEqualTo(productId);
                    assertThat(item.quantity()).isEqualTo(2);
                    assertThat(item.amount()).isEqualTo(9_000L);
                });
    }

    @Test
    void 주문이_실패하면_이벤트도_저장되지_않는다() {
        // given: 잔액 1,000P로 4,500원 상품 주문 → 포인트 부족
        Long userId = 포인트_충전된_사용자_생성(1_000);
        Long productId = 상품_생성(4_500, 10);

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(userId,
                new OrderCreateRequest(List.of(new OrderItemRequest(productId, 1)))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_ENOUGH_POINT);

        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void 취소_이벤트에는_원래_주문_시각과_같은_메시지_키가_담긴다() {
        // given
        Long userId = 포인트_충전된_사용자_생성(10_000);
        Long productId = 상품_생성(4_500, 10);
        Long orderId = orderService.createOrder(userId,
                new OrderCreateRequest(List.of(new OrderItemRequest(productId, 1)))).order().orderId();

        // when
        orderService.cancelOrder(userId, orderId);

        // then
        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).hasSize(2);

        OutboxEvent completed = findByType(events, OrderEventType.ORDER_COMPLETED);
        OutboxEvent canceled = findByType(events, OrderEventType.ORDER_CANCELED);

        // 같은 주문의 이벤트는 같은 key → 같은 파티션 → 완료 후 취소 순서 보장
        assertThat(canceled.getMessageKey()).isEqualTo(completed.getMessageKey());
        assertThat(canceled.getId()).isGreaterThan(completed.getId());

        OrderEventPayload completedPayload = objectMapper.readValue(completed.getPayload(), OrderEventPayload.class);
        OrderEventPayload canceledPayload = objectMapper.readValue(canceled.getPayload(), OrderEventPayload.class);

        // 취소 이벤트의 orderedAt = 원래 주문 시각 (인기 메뉴에서 주문일 키를 찾는 기준)
        assertThat(canceledPayload.orderedAt()).isEqualTo(completedPayload.orderedAt());
        assertThat(canceledPayload.occurredAt()).isAfterOrEqualTo(canceledPayload.orderedAt());
        assertThat(canceledPayload.eventId()).isNotEqualTo(completedPayload.eventId());
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
                .executeAndReturnKey(Map.of(
                        "name", "테스트카테고리-" + UUID.randomUUID()
                ));

        Number productId = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("products")
                .usingGeneratedKeyColumns("id")
                .executeAndReturnKey(Map.of(
                        "category_id", categoryId.longValue(),
                        "name", "테스트상품-" + UUID.randomUUID(),
                        "price", price,
                        "description", "테스트용 상품",
                        "stock", stock,
                        "status", "ON_SALE"
                ));

        return productId.longValue();
    }

    private OutboxEvent findByType(List<OutboxEvent> events, OrderEventType type) {
        return events.stream()
                .filter(e -> e.getEventType().equals(type.name()))
                .findFirst()
                .orElseThrow();
    }
}