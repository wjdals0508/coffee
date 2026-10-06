package com.example.coffee.global.outbox;

/** Outbox에 이벤트가 저장되었음을 알리는 스프링 내부 이벤트 (커밋 후 즉시 발행의 신호) */
public record OutboxEventRecorded(Long outboxEventId) {
}