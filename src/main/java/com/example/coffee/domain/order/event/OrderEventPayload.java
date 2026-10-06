package com.example.coffee.domain.order.event;

import com.example.coffee.domain.order.entity.Order;
import com.example.coffee.domain.order.entity.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public record OrderEventPayload(
        String eventId,
        OrderEventType eventType,
        Long orderId,
        Long userId,
        List<Item> items,
        long totalAmount,
        LocalDateTime orderedAt,
        LocalDateTime occurredAt
) {

    public record Item(
            Long productId,
            int quantity,
            long unitPrice,
            long amount
    ) {
        static Item from(OrderItem orderItem) {
            return new Item(
                    orderItem.getProduct().getId(),
                    orderItem.getQuantity(),
                    orderItem.getUnitPrice(),
                    orderItem.getAmount()
            );
        }
    }

    public static OrderEventPayload of(String eventId,
                                       OrderEventType eventType,
                                       Order order,
                                       List<OrderItem> items,
                                       LocalDateTime occurredAt) {
        return new OrderEventPayload(
                eventId,
                eventType,
                order.getId(),
                order.getUser().getId(),
                items.stream().map(Item::from).toList(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                occurredAt
        );
    }
}