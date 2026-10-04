package com.example.coffee.domain.order.dto.response;

import com.example.coffee.domain.order.entity.Order;
import com.example.coffee.domain.order.entity.OrderItem;
import com.example.coffee.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        OrderStatus status,
        List<OrderItemResponse> items,
        long totalAmount,
        LocalDateTime orderedAt,
        LocalDateTime canceledAt
) {
    public static OrderResponse of(Order order, List<OrderItem> items) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                items.stream().map(OrderItemResponse::from).toList(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getCanceledAt()
        );
    }
}