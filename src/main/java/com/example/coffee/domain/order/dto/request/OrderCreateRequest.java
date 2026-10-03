package com.example.coffee.domain.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderCreateRequest(
        @NotEmpty(message = "주문 항목은 1개 이상이어야 합니다.")
        @Size(max = 20, message = "한 번에 주문할 수 있는 상품 종류는 20개까지입니다.")
        List<@Valid OrderItemRequest> items
) {}