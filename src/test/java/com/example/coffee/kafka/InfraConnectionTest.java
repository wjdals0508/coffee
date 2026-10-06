package com.example.coffee.kafka;

import com.example.coffee.global.config.kafka.KafkaTopicConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class InfraConnectionTest {

    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void Redis에_쓰고_읽을_수_있다() {
        redisTemplate.opsForValue().set("connection-test", "ok", Duration.ofSeconds(10));
        assertThat(redisTemplate.opsForValue().get("connection-test")).isEqualTo("ok");
    }

    @Test
    void Kafka에_메시지를_보낼_수_있다() throws Exception {
        SendResult<String, String> result = kafkaTemplate
                .send(KafkaTopicConfig.ORDER_EVENTS_TOPIC, "test-key", "hello")
                .get(10, TimeUnit.SECONDS);

        assertThat(result.getRecordMetadata().topic()).isEqualTo(KafkaTopicConfig.ORDER_EVENTS_TOPIC);
    }
}