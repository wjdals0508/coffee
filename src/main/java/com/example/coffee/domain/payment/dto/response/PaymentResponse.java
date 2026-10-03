package com.example.coffee.domain.payment.dto.response;

import com.example.coffee.domain.payment.entity.FailReason;
import com.example.coffee.domain.payment.entity.Payment;
import com.example.coffee.domain.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        long amount,
        PaymentStatus status,
        FailReason failReason,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getFailReason(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}