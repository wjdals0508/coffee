package com.example.coffee.global.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class DltMonitorConsumer {

    @KafkaListener(topics = KafkaTopicConfig.ORDER_EVENTS_DLT_TOPIC, groupId = "dlt-monitor")
    public void monitor(ConsumerRecord<String, String> record) {
        log.error("[DLT] 처리 실패 메시지 보관됨 — group={}, originalOffset={}, exception={}, message={}, key={}",
                header(record, KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP),
                header(record, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                header(record, KafkaHeaders.DLT_EXCEPTION_FQCN),
                header(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                record.key());
    }

    private String header(ConsumerRecord<String, String> record, String key) {
        Header header = record.headers().lastHeader(key);
        if (header == null) {
            return null;
        }
        // offset 헤더는 숫자(8바이트)로 저장되어 있어서 문자열로 바로 읽으면 깨짐
        if (KafkaHeaders.DLT_ORIGINAL_OFFSET.equals(key) && header.value().length == Long.BYTES) {
            return String.valueOf(java.nio.ByteBuffer.wrap(header.value()).getLong());
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}