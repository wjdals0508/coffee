package com.example.coffee.domain.point.entity;

import com.example.coffee.domain.order.entity.Order;
import com.example.coffee.domain.user.entity.User;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "point_history",
        indexes = @Index(name = "idx_point_history_user_created", columnList = "user_id, created_at"),
        uniqueConstraints = @UniqueConstraint(name = "uk_point_history_order", columnNames = "order_id")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private PointType type;

    @Column(name = "amount", nullable = false, updatable = false)
    private long amount;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private long balanceAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", updatable = false)
    private Order order;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PointHistory(User user, PointType type, long amount, long balanceAfter, Order order) {
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "회원은 필수입니다.");
        }
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "금액은 0보다 커야 합니다.");
        }
        if (balanceAfter < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "처리 후 잔액은 음수일 수 없습니다.");
        }
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.order = order;
    }

    public static PointHistory charge(User user, long amount, long balanceAfter) {
        return new PointHistory(user, PointType.CHARGE, amount, balanceAfter, null);
    }

    public static PointHistory give(User user, long amount, long balanceAfter) {
        return new PointHistory(user, PointType.GIVE, amount, balanceAfter, null);
    }

    public static PointHistory use(User user, Order order, long amount, long balanceAfter) {
        if (order == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "결제 내역에는 주문이 필수입니다.");
        }
        return new PointHistory(user, PointType.USE, amount, balanceAfter, order);
    }
}