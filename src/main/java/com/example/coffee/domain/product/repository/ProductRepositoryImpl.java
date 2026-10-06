package com.example.coffee.domain.product.repository;

import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import com.example.coffee.domain.product.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.util.List;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QProduct product = QProduct.product;
    private final QCategory category = QCategory.category;

    @Override
    public Page<Product> findProducts(
            Long categoryId,
            ProductStatus excludedStatus,
            Pageable pageable
    ) {
        List<Product> products = queryFactory
                .selectFrom(product)
                .join(product.category, category).fetchJoin()
                .where(
                        categoryIdEq(categoryId),
                        product.status.ne(excludedStatus)
                )
                .orderBy(toOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        categoryIdEq(categoryId),
                        product.status.ne(excludedStatus)
                );

        return PageableExecutionUtils.getPage(
                products,
                pageable,
                () -> {
                    Long count = countQuery.fetchOne();
                    return count == null ? 0L : count;
                }
        );
    }

    private BooleanExpression categoryIdEq(Long categoryId) {
        return categoryId == null ? null : product.category.id.eq(categoryId);
    }

    @Override
    public Page<Product> searchProducts(
            String keyword,
            ProductStatus excludedStatus,
            Pageable pageable
    ) {
        List<Product> products = queryFactory
                .selectFrom(product)
                .join(product.category, category).fetchJoin()
                .where(
                        product.name.contains(keyword),
                        product.status.ne(excludedStatus)
                )
                .orderBy(toOrderSpecifiers(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(product.count())
                .from(product)
                .join(product.category, category)
                .where(
                        product.name.contains(keyword),
                        product.status.ne(excludedStatus)
                );

        return PageableExecutionUtils.getPage(
                products,
                pageable,
                () -> {
                    Long count = countQuery.fetchOne();
                    return count == null ? 0L : count;
                }
        );
    }

    private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort) {
        return sort.stream()
                .map(this::toOrderSpecifier)
                .toArray(OrderSpecifier<?>[]::new);
    }

    private OrderSpecifier<?> toOrderSpecifier(Sort.Order sortOrder) {
        com.querydsl.core.types.Order direction = sortOrder.isAscending()
                ? com.querydsl.core.types.Order.ASC
                : com.querydsl.core.types.Order.DESC;

        return switch (sortOrder.getProperty()) {
            case "id" -> new OrderSpecifier<>(direction, product.id);
            case "price" -> new OrderSpecifier<>(direction, product.price);
            default -> throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "지원하지 않는 상품 정렬 필드입니다: " + sortOrder.getProperty()
            );
        };
    }
}
