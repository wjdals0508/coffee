package com.example.coffee.global.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayScheduler {

    private static final int BATCH_SIZE = 50;

    private final OutboxPublisher outboxPublisher;

    @Scheduled(
            fixedDelayString = "${outbox.relay.fixed-delay-ms:5000}",
            initialDelayString = "${outbox.relay.initial-delay-ms:10000}"
    )
    public void relay() {
        try {
            int processed = outboxPublisher.relayPending(BATCH_SIZE);
            if (processed > 0) {
                log.info("Outbox 재발행 처리: {}건", processed);
            }
        } catch (RuntimeException e) {
            log.error("Outbox 재발행 중 오류", e);
        }
    }
}