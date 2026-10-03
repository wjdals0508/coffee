package com.example.coffee.domain.point.facade;

import com.example.coffee.domain.point.dto.request.PointChargeRequest;
import com.example.coffee.domain.point.dto.response.PointChargeResponse;
import com.example.coffee.domain.point.service.PointService;
import com.example.coffee.global.idempotency.IdempotencyExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PointChargeFacade {

    private static final String NAMESPACE = "charge";

    private final IdempotencyExecutor idempotencyExecutor;
    private final PointService pointService;

    public PointChargeResponse charge(String idempotencyKey, Long userId, PointChargeRequest request) {
        return idempotencyExecutor.execute(
                NAMESPACE, userId, idempotencyKey, PointChargeResponse.class,
                () -> pointService.charge(userId, request.amount())
        );
    }
}