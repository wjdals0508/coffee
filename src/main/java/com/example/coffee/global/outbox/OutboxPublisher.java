package com.example.coffee.global.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    public static final String HEADER_EVENT_ID = "eventId";
    public static final String HEADER_EVENT_TYPE = "eventType";

    private static final int MAX_RETRY = 10;
    private static final long SEND_TIMEOUT_SECONDS = 10;
    private static final Duration RELAY_DELAY = Duration.ofSeconds(10);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxEventRepository outboxEventRepository;

    /** ① 커밋 직후 즉시 발행. 실패해도 예외를 던지지 않음 (PENDING으로 남아 Relay가 처리) */
    public void publishImmediately(Long outboxEventId) {
        OutboxEvent event = outboxEventRepository.findById(outboxEventId).orElse(null);
        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return;
        }

        try {
            send(event).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            outboxEventRepository.markPublishedIfPending(
                    event.getId(), OutboxStatus.PENDING, OutboxStatus.PUBLISHED, LocalDateTime.now());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("즉시 발행 중단, 재발행 대기: outboxId={}", outboxEventId);
        } catch (ExecutionException | TimeoutException e) {
            log.warn("즉시 발행 실패, 재발행 대기: outboxId={}, reason={}", outboxEventId, e.getMessage());
        }
    }

    /** ② 밀린 PENDING 이벤트 재발행. 처리한 건수를 반환 */
    @Transactional
    public int relayPending(int batchSize) {
        List<OutboxEvent> events = outboxEventRepository.findPendingForRelay(
                LocalDateTime.now().minus(RELAY_DELAY), batchSize);
        if (events.isEmpty()) {
            return 0;
        }

        // 전부 먼저 보내고(병렬 전송) 결과를 순서대로 확인
        List<CompletableFuture<SendResult<String, String>>> futures = events.stream()
                .map(this::send)
                .toList();

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < events.size(); i++) {
            OutboxEvent event = events.get(i);
            try {
                futures.get(i).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                event.markPublished(now);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                event.recordFailure("interrupted", MAX_RETRY);
            } catch (ExecutionException | TimeoutException e) {
                event.recordFailure(e.getMessage(), MAX_RETRY);
                if (event.getStatus() == OutboxStatus.FAILED) {
                    log.error("Outbox 재발행 한도 초과 — 수동 확인 필요: outboxId={}, eventId={}",
                            event.getId(), event.getEventId());
                }
            }
        }
        return events.size();
    }

    private CompletableFuture<SendResult<String, String>> send(OutboxEvent event) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(event.getTopic(), event.getMessageKey(), event.getPayload());
        record.headers().add(HEADER_EVENT_ID, event.getEventId().getBytes(StandardCharsets.UTF_8));
        record.headers().add(HEADER_EVENT_TYPE, event.getEventType().getBytes(StandardCharsets.UTF_8));

        try {
            return kafkaTemplate.send(record);
        } catch (RuntimeException e) {
            // 브로커 정보를 못 받는 등 send() 자체가 실패한 경우도 실패한 Future로 통일
            return CompletableFuture.failedFuture(e);
        }
    }
}