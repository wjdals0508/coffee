package com.example.coffee.domain.product.popular.dto;

import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.entity.ProductStatus;

public record PopularMenuResponse(
        int rank,
        Long productId,
        String name,
        long price,
        long orderCount,
        ProductStatus status
) {
    public static PopularMenuResponse of(int rank, Product product, long orderCount) {
        return new PopularMenuResponse(
                rank,
                product.getId(),
                product.getName(),
                product.getPrice(),
                orderCount,
                product.getStatus()
        );
    }
}