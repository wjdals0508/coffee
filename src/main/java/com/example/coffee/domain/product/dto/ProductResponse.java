package com.example.coffee.domain.product.dto;

import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.entity.ProductStatus;

public record ProductResponse(
        Long id,
        String category,
        String name,
        long price,
        String description,
        ProductStatus status
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getPrice(),
                product.getDescription(),
                product.getStatus()
        );
    }
}
