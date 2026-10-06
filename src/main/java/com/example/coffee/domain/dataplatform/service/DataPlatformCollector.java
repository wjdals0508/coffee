package com.example.coffee.domain.dataplatform.service;

import com.example.coffee.domain.dataplatform.dto.OrderEventMessage;
import com.example.coffee.domain.dataplatform.entity.CollectedOrderEvent;
import com.example.coffee.domain.dataplatform.repository.CollectedOrderEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataPlatformCollector {

    private static final String DUPLICATE_CONSTRAINT = "uk_collected_event_id";

    private final CollectedOrderEventRepository collectedOrderEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * 주문 이벤트 적재. 이미 받은 이벤트면 무시하고 정상 종료한다.
     * @return 새로 적재했으면 true, 중복이면 false
     */
    public boolean collect(String payload) {
        OrderEventMessage message = objectMapper.readValue(payload, OrderEventMessage.class);

        // 1차: 대부분의 재수신은 여기서 걸러짐
        if (collectedOrderEventRepository.existsByEventId(message.eventId())) {
            log.info("[DATA PLATFORM] 중복 수신 무시: eventId={}", message.eventId());
            return false;
        }

        try {
            collectedOrderEventRepository.saveAndFlush(CollectedOrderEvent.of(
                    message.eventId(),
                    message.eventType(),
                    message.orderId(),
                    message.userId(),
                    message.totalAmount(),
                    message.occurredAt(),
                    payload
            ));
        } catch (DataIntegrityViolationException e) {
            // 2차: 같은 이벤트가 거의 동시에 두 번 온 경우 (확인과 저장 사이에 다른 쪽이 먼저 저장)
            if (isDuplicate(e)) {
                log.info("[DATA PLATFORM] 동시 중복 수신 무시: eventId={}", message.eventId());
                return false;
            }
            throw e;
        }

        log.info("[DATA PLATFORM] 적재 완료: eventId={}, type={}, orderId={}, userId={}, amount={}",
                message.eventId(), message.eventType(), message.orderId(), message.userId(), message.totalAmount());
        return true;
    }

    private boolean isDuplicate(DataIntegrityViolationException e) {
        String cause = e.getMostSpecificCause().getMessage();
        return cause != null && cause.contains(DUPLICATE_CONSTRAINT);
    }
}