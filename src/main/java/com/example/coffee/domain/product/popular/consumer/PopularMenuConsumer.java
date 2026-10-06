package com.example.coffee.domain.product.popular.consumer;

import com.example.coffee.domain.product.popular.service.PopularMenuAggregator;
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
@ConditionalOnProperty(name = "popular-menu.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class PopularMenuConsumer {

    public static final String GROUP_ID = "popular-menu";

    private final PopularMenuAggregator aggregator;

    @KafkaListener(
            topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC,
            groupId = GROUP_ID,
            concurrency = "3"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.debug("[POPULAR] 수신: partition={}, offset={}, key={}",
                record.partition(), record.offset(), record.key());
        aggregator.aggregate(record.value());
    }
}