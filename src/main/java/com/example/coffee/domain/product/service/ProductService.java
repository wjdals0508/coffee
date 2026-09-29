package com.example.coffee.domain.product.service;

import com.example.coffee.domain.product.dto.ProductResponse;
import com.example.coffee.domain.product.dto.ProductSort;
import com.example.coffee.domain.product.entity.ProductStatus;
import com.example.coffee.domain.product.repository.ProductRepository;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import com.example.coffee.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
                                ProductStatus.DISCONTINUED,
                                sortedPageable
                        )
                        .map(ProductResponse::from)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(
            String keyword,
            ProductSort sort,
            Pageable pageable
    ) {
        String normalizedKeyword = normalizeRequiredKeyword(keyword);

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort.toSort()
        );

        return PageResponse.from(
                productRepository.searchProducts(
                                normalizedKeyword,
                                ProductStatus.DISCONTINUED,
                                sortedPageable
                        )
                        .map(ProductResponse::from)
        );
    }

    private String normalizeRequiredCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "카테고리는 필수입니다.");
        }
        return category.trim();
    }

    private String normalizeRequiredKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "검색어는 필수입니다.");
        }
        return keyword.trim();
    }
}
