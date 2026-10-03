package com.example.coffee.domain.payment.entity;

import com.example.coffee.domain.user.entity.User;
import com.example.coffee.global.entity.BaseTimeEntity;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "portone_payment_id", nullable = false, length = 200, unique = true)
    private String portonePaymentId;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_reason", length = 50)
    private FailReason failReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    private Payment(User user, Long amount) {
        if (user == null || amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        this.user = user;
        this.portonePaymentId = generatePortonePaymentId();
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    private static String generatePortonePaymentId() {
        return "pay_" + UUID.randomUUID();
    }

    public void complete() {
        changeStatus(PaymentStatus.COMPLETED);
        this.paidAt = LocalDateTime.now();
    }

    public void fail(FailReason reason) {
        changeStatus(PaymentStatus.FAILED);
        this.failReason = reason;
    }

    public void cancel() {
        changeStatus(PaymentStatus.CANCELLED);
    }

    public void fullRefund() {
        changeStatus(PaymentStatus.FULL_REFUND);
    }

    private void changeStatus(PaymentStatus target) {
        if (!this.status.canTransitTo(target)) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
        this.status = target;
    }
}
