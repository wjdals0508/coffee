package com.example.coffee.domain.product.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import com.example.coffee.domain.product.entity.*;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QProduct product = QProduct.product;
    private final QCategory category = QCategory.category;

    @Override
    public Page<Product> findProducts(
            String categoryName,
            ProductStatus status,
            Pageable pageable
    ) {
        List<Product> products = queryFactory
                .selectFrom(product)
                .join(product.category, category).fetchJoin()
                .where(
                        parentCategory.name.eq(categoryName),
                        subCategoryEq(subCategoryName),
                        product.tier.eq(tier),
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
                .join(category.parent, parentCategory)
                .where(
                        parentCategory.name.eq(categoryName),
                        subCategoryEq(subCategoryName),
                        product.tier.eq(tier),
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

}
