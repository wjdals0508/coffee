package com.example.coffee.domain.product.repository;

import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductRepositoryCustom {

    Page<Product> findProducts(
            String category,
            ProductStatus status,
            Pageable pageable
    );



}
