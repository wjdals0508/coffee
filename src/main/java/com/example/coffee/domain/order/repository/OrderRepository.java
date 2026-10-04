package com.example.coffee.domain.order.repository;

import com.example.coffee.domain.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    /*
    락이 없으면

    [취소 버튼 두 번]
    요청 A: 주문 조회(ORDERED) → 취소 → 환불
    요청 B: 주문 조회(ORDERED) → 취소 → 환불   ← 락이 없으면 둘 다 ORDERED를 봄

    [취소 vs 제조 시작]
    사용자: 주문 조회(ORDERED) → 취소 가능 → 환불
    매장:   주문 조회(ORDERED) → 제조 시작      ← 커피는 만들어지는데 돈은 환불됨
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :orderId and o.user.id = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("orderId") Long orderId,
                                               @Param("userId") Long userId);
}