package com.example.coffee.domain.payment.dto.response;


import com.example.coffee.domain.payment.entity.Payment;

public record PaymentCancelResponse(
        Long paymentId,
        String portonePaymentId,
        String paymentStatus,
        String message
) {
    public static PaymentCancelResponse from(Payment payment, String message) {
        return new PaymentCancelResponse(
                payment.getId(),
                payment.getPortonePaymentId(),
                payment.getStatus().name(),
                message
        );
    }
}