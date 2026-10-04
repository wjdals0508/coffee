package com.example.coffee.domain.point.entity;

import com.example.coffee.domain.user.entity.User;
import com.example.coffee.global.entity.BaseTimeEntity;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_point")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint extends BaseTimeEntity {

    private static final long MAX_CHARGE_AMOUNT = 1_000_000L;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "balance", nullable = false)
    private long balance;

    private UserPoint(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "회원은 필수입니다.");
        }
        this.user = user;
        this.balance = 0L;
    }

    public static UserPoint create(User user) {
        return new UserPoint(user);
    }

    // 충전
    public void charge(long amount) {
        validatePositive(amount);
        if (amount > MAX_CHARGE_AMOUNT) {
            throw new BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT,
                    "1회 최대 충전 금액은 " + String.format("%,d", MAX_CHARGE_AMOUNT) + "P입니다.");
        }
        increase(amount);
    }

    // 운영자 지급
    public void give(long amount) {
        validatePositive(amount);
        increase(amount);
    }

    // 포인트 사용
    public void use(long amount) {
        validatePositive(amount);
        if (this.balance < amount) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_POINT, "포인트 잔액이 부족합니다.");
        }
        this.balance -= amount;
    }

    private void increase(long amount) {
        this.balance = Math.addExact(this.balance, amount);
    }

    private static void validatePositive(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "금액은 0보다 커야 합니다.");
        }
    }

    // 주문 취소 환불
    public void refund(long amount) {
        validatePositive(amount);
        increase(amount);
    }
}