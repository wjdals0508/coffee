package com.example.coffee.domain.order.entity;

import com.example.coffee.domain.user.entity.User;
import com.example.coffee.global.entity.BaseTimeEntity;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_created_at", columnList = "created_at"),
                @Index(name = "idx_orders_user_created", columnList = "user_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "total_amount", nullable = false, updatable = false)
    private long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    private Order(User user, List<OrderItem> items) {
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "회원은 필수입니다.");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "주문 항목은 1개 이상이어야 합니다.");
        }
        this.user = user;
        this.totalAmount = calculateTotalAmount(items);
        this.status = OrderStatus.ORDERED;
    }

    public static Order create(User user, List<OrderItem> items) {
        Order order = new Order(user, items);
        items.forEach(item -> item.assignOrder(order));
        return order;
    }

    public void cancel() {
        if (this.status == OrderStatus.CANCELED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELED);
        }
        if (!this.status.canTransitTo(OrderStatus.CANCELED)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELABLE);
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
    }

    public void startPreparing() {
        changeStatus(OrderStatus.PREPARING);
    }

    public void complete() {
        changeStatus(OrderStatus.COMPLETED);
    }

    private void changeStatus(OrderStatus target) {
        if (!this.status.canTransitTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = target;
    }

    private static long calculateTotalAmount(List<OrderItem> items) {
        long total = 0L;
        for (OrderItem item : items) {
            total = Math.addExact(total, item.getAmount());
        }
        return total;
    }
}