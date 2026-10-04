package com.example.coffee.domain.product.repository;

import com.example.coffee.domain.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    /*
    [주문 A: 사용자 1, 상품 3·7]  상품3 → 상품7 → 포인트(사용자1)
    [주문 B: 사용자 2, 상품 7·3]  상품3 → 상품7 → 포인트(사용자2)
                           ↑ 요청 순서와 관계없이 id 오름차순
    */

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :productIds order by p.id")
    List<Product> findAllByIdInForUpdate(@Param("productIds") Collection<Long> productIds);
}