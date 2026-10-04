package com.example.coffee.domain.cart.repository;

import com.example.coffee.domain.cart.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    @EntityGraph(attributePaths = "product")
    List<CartItem> findAllByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    @EntityGraph(attributePaths = "product")
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("delete from CartItem c where c.user.id = :userId and c.id in :cartItemIds")
    int deleteAllByUserIdAndIdIn(@Param("userId") Long userId,
                                 @Param("cartItemIds") Collection<Long> cartItemIds);
}