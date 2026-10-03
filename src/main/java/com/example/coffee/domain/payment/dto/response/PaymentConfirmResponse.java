package com.example.coffee.domain.payment.dto.response;


import com.example.coffee.domain.payment.entity.Payment;

public record PaymentConfirmResponse(
        Long paymentId,
        Long amount,
        String paymentStatus,
        String message
) {
    // Payment 엔티티만 넘겼을 때의 기본 생성 메서드
    public static PaymentConfirmResponse from(Payment payment) {
        return new PaymentConfirmResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getStatus().name(),
                "결제가 성공적으로 완료되었습니다."
        );
    }

    // 메시지나 주문 상태를 직접 지정하고 싶을 때 쓰는 오버로딩 메서드
    public static PaymentConfirmResponse of(Payment payment, String message) {
        return new PaymentConfirmResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getStatus().name(),
                message
        );
    }
}