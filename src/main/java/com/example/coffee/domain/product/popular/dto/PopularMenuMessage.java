package com.example.coffee.domain.product.popular.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PopularMenuMessage(
        String eventId,
        String eventType,
        List<Item> items,
        LocalDateTime orderedAt
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            Long productId,
            int quantity
    ) {
    }
}