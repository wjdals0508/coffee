package com.example.coffee.domain.dataplatform.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "collected_order_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_collected_event_id", columnNames = "event_id"),
        indexes = @Index(name = "idx_collected_order", columnList = "order_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectedOrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36, updatable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private String eventType;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "total_amount", nullable = false, updatable = false)
    private long totalAmount;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "payload", nullable = false, columnDefinition = "json", updatable = false)
    private String payload;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private LocalDateTime collectedAt;

    private CollectedOrderEvent(String eventId, String eventType, Long orderId, Long userId,
                                long totalAmount, LocalDateTime occurredAt, String payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.occurredAt = occurredAt;
        this.payload = payload;
        this.collectedAt = LocalDateTime.now();
    }

    public static CollectedOrderEvent of(String eventId, String eventType, Long orderId, Long userId,
                                         long totalAmount, LocalDateTime occurredAt, String payload) {
        return new CollectedOrderEvent(eventId, eventType, orderId, userId, totalAmount, occurredAt, payload);
    }
}