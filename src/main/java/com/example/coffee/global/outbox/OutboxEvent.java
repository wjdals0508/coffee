package com.example.coffee.global.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "outbox_events",
        indexes = @Index(name = "idx_outbox_status_created", columnList = "status, created_at"),
        uniqueConstraints = @UniqueConstraint(name = "uk_outbox_event_id", columnNames = "event_id")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 36, updatable = false)
    private String eventId;

    @Column(name = "topic", nullable = false, length = 100, updatable = false)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 100, updatable = false)
    private String messageKey;

    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "json", updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private OutboxEvent(String eventId, String topic, String messageKey, String eventType, String payload) {
        this.eventId = eventId;
        this.topic = topic;
        this.messageKey = messageKey;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    public static OutboxEvent create(String eventId, String topic, String messageKey,
                                     String eventType, String payload) {
        return new OutboxEvent(eventId, topic, messageKey, eventType, payload);
    }

    public void markPublished(LocalDateTime now) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
    }

    public void recordFailure(String error, int maxRetry) {
        this.retryCount++;
        this.lastError = error == null ? null
                : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
        if (this.retryCount >= maxRetry) {
            this.status = OutboxStatus.FAILED;
        }
    }
}