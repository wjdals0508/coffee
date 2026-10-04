package com.example.coffee.domain.order.entity;

import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "order_items",
        indexes = @Index(name = "idx_order_items_order", columnList = "order_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(name = "unit_price", nullable = false, updatable = false)
    private long unitPrice;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    private OrderItem(Product product, int quantity) {
        if (product == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "상품은 필수입니다.");
        }
        product.validatePurchasable(quantity);

        this.product = product;
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }

    public long getAmount() {
        return Math.multiplyExact(unitPrice, quantity);
    }

    public static OrderItem of(Product product, int quantity) {
        return new OrderItem(product, quantity);
    }

    void assignOrder(Order order) {
        this.order = order;
    }
}