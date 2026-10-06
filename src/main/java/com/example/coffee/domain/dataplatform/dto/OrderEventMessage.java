package com.example.coffee.domain.dataplatform.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderEventMessage(
        String eventId,
        String eventType,
        Long orderId,
        Long userId,
        List<Item> items,
        long totalAmount,
        LocalDateTime orderedAt,
        LocalDateTime occurredAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Long productId,
            int quantity,
            long unitPrice,
            long amount
    ) {
    }
}