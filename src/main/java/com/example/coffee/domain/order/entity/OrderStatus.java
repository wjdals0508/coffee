package com.example.coffee.domain.order.entity;

public enum OrderStatus {

    ORDERED {           // 주문 + 포인트 결제 완료
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return target == PREPARING || target == CANCELED;
        }
    },
    PREPARING {         // 제조 중 → 이때부터 취소 불가
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return target == COMPLETED;
        }
    },
    COMPLETED {         // 픽업 완료
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return false;
        }
    },
    CANCELED {
        @Override
        public boolean canTransitTo(OrderStatus target) {
            return false;
        }
    };

    public abstract boolean canTransitTo(OrderStatus target);
}