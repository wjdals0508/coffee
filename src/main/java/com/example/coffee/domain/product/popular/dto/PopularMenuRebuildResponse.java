package com.example.coffee.domain.product.popular.dto;

import java.time.LocalDate;

public record PopularMenuRebuildResponse(
        LocalDate fromDate,
        LocalDate toDate,
        int salesRecordCount
) {
}