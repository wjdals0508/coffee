package com.example.coffee.domain.cart.repository;

import com.example.coffee.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    @EntityGraph(attributePaths = {"product", "product.category"})
    List<CartItem> findAllByUserIdOrderByCreatedAtAsc(Long memberId);

    Optional<CartItem> findByIdAndUserId(Long id, Long memberId);

    List<CartItem> findAllByUserIdAndIdIn(Long memberId, List<Long> cartItemIds);
}
