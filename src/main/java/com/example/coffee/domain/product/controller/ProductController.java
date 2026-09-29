package com.example.coffee.domain.product.controller;

import com.example.coffee.domain.product.dto.ProductResponse;
import com.example.coffee.domain.product.dto.ProductSort;
import com.example.coffee.domain.product.service.ProductService;
import com.example.coffee.global.response.ApiResponse;
import com.example.coffee.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // 카테고리 별 상품 불러오기
    @GetMapping("")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            Authentication authentication,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            Pageable pageable
    ) {

        ProductSort productSort = ProductSort.fromValue(sort);

        PageResponse<ProductResponse> response = productService.getProducts(
                category,
                productSort,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // 상품 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            Pageable pageable
    ) {
        ProductSort productSort = ProductSort.fromValue(sort);

        PageResponse<ProductResponse> response = productService.searchProducts(
                keyword,
                productSort,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }




}
