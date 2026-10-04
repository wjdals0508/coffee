package com.example.coffee.domain.payment.gateway;

public interface PaymentGateway {

    /** 결제 승인. 실패 시 예외를 던진다. */
    void approve(String paymentKey, long amount);
}