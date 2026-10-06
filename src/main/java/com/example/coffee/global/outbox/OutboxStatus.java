package com.example.coffee.global.outbox;

public enum OutboxStatus {
    PENDING,    // 저장됨, 아직 발행 안 됨
    PUBLISHED,  // Kafka 전송 성공
    FAILED      // 재시도 한도 초과 → 운영자 확인 필요
}