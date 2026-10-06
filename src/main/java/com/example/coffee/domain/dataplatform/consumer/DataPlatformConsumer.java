package com.example.coffee.domain.dataplatform.consumer;

import com.example.coffee.domain.dataplatform.service.DataPlatformCollector;
import com.example.coffee.global.config.kafka.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "data-platform.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class DataPlatformConsumer {

    public static final String GROUP_ID = "data-platform";

    private final DataPlatformCollector collector;

    @KafkaListener(
            topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC,
            groupId = GROUP_ID,
            concurrency = "3"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.debug("[DATA PLATFORM] 수신: partition={}, offset={}, key={}",
                record.partition(), record.offset(), record.key());
        collector.collect(record.value());
    }
}