package com.example.coffee.domain.payment.port;

public record PaymentGatewayResponse(
        String id,
        String status,
        long totalAmount,
        long cancelledAmount // 총 취소 금액
) {}