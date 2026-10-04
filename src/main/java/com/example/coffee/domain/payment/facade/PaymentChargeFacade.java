package com.example.coffee.domain.payment.facade;

import com.example.coffee.domain.payment.service.PaymentService;
import com.example.coffee.domain.point.dto.request.PointChargeRequest;
import com.example.coffee.domain.point.dto.response.PointChargeResponse;
import com.example.coffee.global.idempotency.IdempotencyExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentChargeFacade {

    private static final String NAMESPACE = "charge";

    private final IdempotencyExecutor idempotencyExecutor;
    private final PaymentService paymentService;

    public PointChargeResponse charge(String idempotencyKey, Long userId, PointChargeRequest request) {
        return idempotencyExecutor.execute(
                NAMESPACE, userId, idempotencyKey, PointChargeResponse.class,
                () -> paymentService.chargePoint(userId, request.amount())
        );
    }
}