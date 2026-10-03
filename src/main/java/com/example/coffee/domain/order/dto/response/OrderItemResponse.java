package com.example.coffee.domain.order.dto.response;

import com.example.coffee.domain.order.entity.OrderItem;

public record OrderItemResponse(
        Long productId,
        String name,
        long unitPrice,
        int quantity,
        long amount
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getAmount()
        );
    }
}