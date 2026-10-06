package com.example.coffee.domain.product.dto;

import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Locale;

public enum ProductSort {
    POPULAR,
    PRICE_ASC,
    PRICE_DESC,
    NEWEST;

    public static ProductSort fromValue(String value) {
        if (value == null || value.isBlank()) {
            return POPULAR;
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(sort -> sort.name().equals(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "지원하지 않는 상품 정렬 방식입니다: " + value
                ));
    }

    public Sort toSort() {
        return switch (this) {
            case POPULAR -> Sort.by(Sort.Order.asc("id"));
            case PRICE_ASC -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
            case PRICE_DESC -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
            case NEWEST -> Sort.by(Sort.Order.desc("id"));
        };
    }
}
