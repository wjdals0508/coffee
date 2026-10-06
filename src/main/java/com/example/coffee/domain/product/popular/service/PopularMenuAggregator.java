package com.example.coffee.domain.product.popular.service;

import com.example.coffee.domain.product.popular.dto.PopularMenuMessage;
import com.example.coffee.domain.product.popular.repository.PopularMenuRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PopularMenuAggregator {

    private static final String ORDER_COMPLETED = "ORDER_COMPLETED";
    private static final String ORDER_CANCELED = "ORDER_CANCELED";
    private static final int AGGREGATION_DAYS = 7;

    private final PopularMenuRedisRepository popularMenuRedisRepository;
    private final ObjectMapper objectMapper;

    /**
     * 주문 이벤트를 인기 메뉴 점수에 반영한다.
     * @return 실제로 반영했으면 true, 건너뛰었거나 중복이면 false
     */
    public boolean aggregate(String payload) {
        PopularMenuMessage message = objectMapper.readValue(payload, PopularMenuMessage.class);

        int sign = switch (message.eventType()) {
            case ORDER_COMPLETED -> 1;
            case ORDER_CANCELED -> -1;
            default -> 0;
        };
        if (sign == 0) {
            log.debug("[POPULAR] 집계 대상이 아닌 이벤트: type={}", message.eventType());
            return false;
        }

        LocalDate orderDate = message.orderedAt().toLocalDate();
        if (orderDate.isBefore(LocalDate.now().minusDays(AGGREGATION_DAYS))) {
            log.debug("[POPULAR] 집계 기간이 지난 주문: eventId={}, orderDate={}", message.eventId(), orderDate);
            return false;
        }

        Map<Long, Long> deltaByProductId = new LinkedHashMap<>();
        for (PopularMenuMessage.Item item : message.items()) {
            deltaByProductId.merge(item.productId(), (long) sign * item.quantity(), Long::sum);
        }

        boolean applied = popularMenuRedisRepository.applyOnce(message.eventId(), orderDate, deltaByProductId);
        if (!applied) {
            log.info("[POPULAR] 중복 수신 무시: eventId={}", message.eventId());
        }
        return applied;
    }
}