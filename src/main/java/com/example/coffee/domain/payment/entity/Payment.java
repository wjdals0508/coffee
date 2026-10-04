package com.example.coffee.domain.payment.entity;

import com.example.coffee.domain.user.entity.User;
import com.example.coffee.global.entity.BaseTimeEntity;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        indexes = @Index(name = "idx_payments_user_created", columnList = "user_id, created_at")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "payment_key", nullable = false, length = 64, unique = true, updatable = false)
    private String paymentKey;

    @Column(name = "amount", nullable = false, updatable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    private Payment(User user, long amount) {
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "회원은 필수입니다.");
        }
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "결제 금액은 0보다 커야 합니다.");
        }
        this.user = user;
        this.paymentKey = "pay_" + UUID.randomUUID();
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public static Payment create(User user, long amount) {
        return new Payment(user, amount);
    }

    public void complete() {
        if (!this.status.canTransitTo(PaymentStatus.COMPLETED)) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
    }
}