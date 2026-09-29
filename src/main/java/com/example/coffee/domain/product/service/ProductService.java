package com.example.coffee.domain.product.service;

import com.example.coffee.domain.product.dto.ProductResponse;
import com.example.coffee.domain.product.dto.ProductSort;
import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.repository.ProductRepository;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import com.example.coffee.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProducts(
            String category,
            ProductSort sort,
            Pageable pageable
    ) {
        String normalizedCategory = normalizeRequiredCategory(category);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort.toSort()
        );

        return PageResponse.from(
                productRepository.findProducts(
                                normalizedCategory,
                                normalizedSubCategory,
                                requestedTier,
                                ProductStatus.DISCONTINUED,
                                sortedPageable
                        )
                        .map(ProductResponse::from)
        );
    }




}
