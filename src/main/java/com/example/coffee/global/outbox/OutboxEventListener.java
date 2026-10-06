package com.example.coffee.global.outbox;

import com.example.coffee.global.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventListener {

    private final OutboxPublisher outboxPublisher;

    @Async(AsyncConfig.OUTBOX_PUBLISH_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxEventRecorded(OutboxEventRecorded event) {
        outboxPublisher.publishImmediately(event.outboxEventId());
    }
}