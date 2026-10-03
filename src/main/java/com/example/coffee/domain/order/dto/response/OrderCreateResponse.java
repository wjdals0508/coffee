package com.example.coffee.domain.order.dto.response;

public record OrderCreateResponse(
        OrderResponse order,
        long remainingPoint
) {}