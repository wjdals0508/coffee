package com.example.coffee.domain.product.controller;

import com.example.coffee.domain.product.dto.ProductResponse;
import com.example.coffee.domain.product.dto.ProductSort;
import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.service.ProductService;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import com.example.coffee.global.response.ApiResponse;
import com.example.coffee.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/api/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            Authentication authentication,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            Pageable pageable
    ) {

        ProductSort productSort = ProductSort.fromValue(sort);
        List<Product> result = productService.getProducts(
                category,
                productSort,
                pageable
        )

        return ResponseEntity.ok(ApiResponse.ok(

        ));
    }





}
