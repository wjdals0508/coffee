package com.example.coffee.domain.order.event;

import com.example.coffee.domain.order.entity.Order;
import com.example.coffee.domain.order.entity.OrderItem;
import com.example.coffee.global.config.kafka.KafkaTopicConfig;
import com.example.coffee.global.outbox.OutboxEvent;
import com.example.coffee.global.outbox.OutboxEventRecorded;
import com.example.coffee.global.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** 주문 완료 이벤트 기록 — 반드시 주문 생성 트랜잭션 안에서 호출 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordOrderCompleted(Order order, List<OrderItem> items) {
        record(OrderEventType.ORDER_COMPLETED, order, items, order.getCreatedAt());
    }

    /** 주문 취소 이벤트 기록 — 반드시 주문 취소 트랜잭션 안에서 호출 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordOrderCanceled(Order order, List<OrderItem> items) {
        record(OrderEventType.ORDER_CANCELED, order, items, order.getCanceledAt());
    }

    private void record(OrderEventType eventType, Order order, List<OrderItem> items, LocalDateTime occurredAt) {
        String eventId = UUID.randomUUID().toString();
        OrderEventPayload payload = OrderEventPayload.of(eventId, eventType, order, items, occurredAt);

        OutboxEvent outboxEvent = outboxEventRepository.save(OutboxEvent.create(
                eventId,
                KafkaTopicConfig.ORDER_EVENTS_TOPIC,
                String.valueOf(order.getId()),
                eventType.name(),
                objectMapper.writeValueAsString(payload)
        ));

        eventPublisher.publishEvent(new OutboxEventRecorded(outboxEvent.getId()));
    }
}