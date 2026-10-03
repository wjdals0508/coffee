package com.example.coffee.domain.order.repository;

import com.example.coffee.domain.order.entity.OrderItem;
import com.example.coffee.domain.order.repository.dto.DailyProductSales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            select i from OrderItem i
            join fetch i.product
            where i.order.id = :orderId
            """)
    List<OrderItem> findAllWithProductByOrderId(@Param("orderId") Long orderId);

    @Query("""
            select i from OrderItem i
            join fetch i.product
            where i.order.id in :orderIds
            """)
    List<OrderItem> findAllWithProductByOrderIdIn(@Param("orderIds") List<Long> orderIds);

    @Query("""
            select new com.example.coffee.domain.order.repository.dto.DailyProductSales(
                cast(o.createdAt as LocalDate), i.product.id, sum(i.quantity))
            from OrderItem i
            join i.order o
            where o.createdAt >= :from
              and o.createdAt < :to
            group by cast(o.createdAt as LocalDate), i.product.id
            """)
    List<DailyProductSales> sumDailySales(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}