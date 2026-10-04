package com.example.coffee.domain.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "payment.gateway", havingValue = "fake")
public class FakePaymentGateway implements PaymentGateway {

    @Override
    public void approve(String paymentKey, long amount) {
        log.info("[FAKE PAYMENT] 검증 없이 승인. paymentKey={}, amount={}", paymentKey, amount);
    }
}