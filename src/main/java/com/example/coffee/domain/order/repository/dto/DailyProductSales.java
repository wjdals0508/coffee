package com.example.coffee.domain.order.repository.dto;

import java.time.LocalDate;

public record DailyProductSales(
        LocalDate orderDate,
        Long productId,
        Long quantity
) {}