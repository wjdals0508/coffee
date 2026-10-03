package com.example.coffee.global.idempotency;

import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyExecutor {

    private static final String KEY_PREFIX = "idempotency:";
    private static final String PROCESSING = "PROCESSING";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> T execute(String namespace, Long userId, String idempotencyKey,
                         Class<T> responseType, Supplier<T> action) {
        String key = KEY_PREFIX + namespace + ":" + userId + ":" + idempotencyKey;

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, PROCESSING, TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            return getPreviousResult(key, responseType);
        }

        T response;
        try {
            response = action.get();
        } catch (RuntimeException e) {
            redisTemplate.delete(key);
            throw e;
        }

        saveResult(key, response);
        return response;
    }

    private <T> T getPreviousResult(String key, Class<T> responseType) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null || PROCESSING.equals(value)) {
            throw new BusinessException(ErrorCode.REQUEST_IN_PROGRESS, "동일한 요청을 처리 중입니다.");
        }
        return objectMapper.readValue(value, responseType);
    }

    private void saveResult(String key, Object response) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response), TTL);
        } catch (RuntimeException e) {
            log.warn("멱등성 결과 저장 실패. key={}", key, e);
        }
    }
}