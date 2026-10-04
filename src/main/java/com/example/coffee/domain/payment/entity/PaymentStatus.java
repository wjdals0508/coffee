package com.example.coffee.domain.payment.entity;

/**
 * 결제 상태
 * - PENDING → COMPLETED : 결제 승인 완료
 */
public enum PaymentStatus {

    PENDING {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return target == COMPLETED;
        }
    },
    COMPLETED {
        @Override
        public boolean canTransitTo(PaymentStatus target) {
            return false;
        }
    };

    public abstract boolean canTransitTo(PaymentStatus target);
}