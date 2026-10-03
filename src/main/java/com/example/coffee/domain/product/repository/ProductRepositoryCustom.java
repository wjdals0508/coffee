package com.example.coffee.domain.product.repository;

import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {

    Page<Product> findProducts(
            String category,
            ProductStatus excludedStatus,
            Pageable pageable
    );

    Page<Product> searchProducts(
            String keyword,
            ProductStatus excludedStatus,
            Pageable pageable
    );
}
