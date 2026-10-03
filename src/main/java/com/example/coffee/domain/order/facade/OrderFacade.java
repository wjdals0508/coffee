package com.example.coffee.domain.order.facade;

import com.example.coffee.domain.order.dto.request.OrderCreateRequest;
import com.example.coffee.domain.order.dto.response.OrderCreateResponse;
import com.example.coffee.domain.order.service.OrderService;
import com.example.coffee.global.idempotency.IdempotencyExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private static final String NAMESPACE = "order";

    private final IdempotencyExecutor idempotencyExecutor;
    private final OrderService orderService;

    public OrderCreateResponse createOrder(String idempotencyKey, Long userId, OrderCreateRequest request) {
        return idempotencyExecutor.execute(
                NAMESPACE, userId, idempotencyKey, OrderCreateResponse.class,
                () -> orderService.createOrder(userId, request)
        );
    }
}