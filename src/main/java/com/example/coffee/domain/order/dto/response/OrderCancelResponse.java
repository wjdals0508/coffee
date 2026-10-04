package com.example.coffee.domain.order.dto.response;

public record OrderCancelResponse(
        OrderResponse order,
        long refundedAmount,
        long remainingPoint
) {}