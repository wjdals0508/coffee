package com.example.coffee.domain.product.popular.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class PopularMenuRedisRepository {

    public static final Duration KEY_TTL = Duration.ofDays(8);

    private static final String RANKING_KEY_PREFIX = "popular:products:";
    private static final String PROCESSED_KEY_PREFIX = "processed:popular:";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;   // yyyyMMdd

    /**
     * KEYS[1] = processed:popular:{eventId}
     * KEYS[2] = popular:products:{yyyyMMdd}
     * ARGV[1] = TTL(초), 이후 (productId, delta) 쌍 반복
     * 반환: 1 = 반영함, 0 = 이미 처리한 이벤트
     */
    private static final RedisScript<Long> APPLY_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('SET', KEYS[1], '1', 'NX', 'EX', ARGV[1]) == false then
              return 0
            end
            for i = 2, #ARGV, 2 do
              redis.call('ZINCRBY', KEYS[2], ARGV[i + 1], ARGV[i])
            end
            redis.call('EXPIRE', KEYS[2], ARGV[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * 이벤트 하나의 점수 변화를 원자적으로 반영한다.
     * @param deltaByProductId 상품 ID → 증감량 (주문 +, 취소 -)
     * @return 새로 반영했으면 true, 이미 처리한 이벤트면 false
     */
    public boolean applyOnce(String eventId, LocalDate orderDate, Map<Long, Long> deltaByProductId) {
        List<String> keys = List.of(processedKey(eventId), rankingKey(orderDate));

        List<String> args = new ArrayList<>();
        args.add(String.valueOf(KEY_TTL.toSeconds()));
        deltaByProductId.forEach((productId, delta) -> {
            args.add(String.valueOf(productId));
            args.add(String.valueOf(delta));
        });

        Long result = redisTemplate.execute(APPLY_SCRIPT, keys, args.toArray());
        return result != null && result == 1L;
    }

    /** 특정 날짜, 특정 상품의 점수 (확인·테스트용) */
    public double getScore(LocalDate orderDate, Long productId) {
        Double score = redisTemplate.opsForZSet().score(rankingKey(orderDate), String.valueOf(productId));
        return score == null ? 0 : score;
    }

    /**
     * 여러 날짜의 랭킹을 합산한다. 키가 없는 날짜는 0으로 취급된다.
     * @return 상품 ID → 기간 합계 판매 수량
     */
    public Map<Long, Long> sumQuantities(List<LocalDate> dates) {
        List<String> keys = dates.stream()
                .map(PopularMenuRedisRepository::rankingKey)
                .toList();

        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .unionWithScores(keys.get(0), keys.subList(1, keys.size()));

        Map<Long, Long> quantityByProductId = new HashMap<>();
        if (tuples == null) {
            return quantityByProductId;
        }

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            if (tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            try {
                quantityByProductId.put(Long.valueOf(tuple.getValue()), Math.round(tuple.getScore()));
            } catch (NumberFormatException e) {
                // 상품 ID가 아닌 멤버는 무시
            }
        }
        return quantityByProductId;
    }

    public static String rankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DATE_FORMAT);
    }

    private static String processedKey(String eventId) {
        return PROCESSED_KEY_PREFIX + eventId;
    }
}