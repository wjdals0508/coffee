package com.example.coffee.domain.product.repository;

import jakarta.persistence.LockModeType;
import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :productIds order by p.id")
    List<Product> findAllByIdInForUpdate(@Param("productIds") List<Long> productIds);
}
