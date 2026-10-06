package com.example.coffee.global.config.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import tools.jackson.core.JacksonException;

@Slf4j
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        record.topic() + KafkaTopicConfig.DLT_SUFFIX, record.partition())
        );

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(4);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // 재시도해도 결과가 같은 실패 → 바로 DLT
        errorHandler.addNotRetryableExceptions(
                JacksonException.class,          // JSON 형식 오류
                IllegalArgumentException.class,  // 잘못된 값
                NullPointerException.class       // 필수 필드 누락
        );

        errorHandler.setRetryListeners((record, exception, attempt) ->
                log.warn("[KAFKA] 처리 실패, 재시도 {}회차: topic={}, partition={}, offset={}, reason={}",
                        attempt, record.topic(), record.partition(), record.offset(), exception.getMessage()));

        return errorHandler;
    }
}