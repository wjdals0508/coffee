package com.example.coffee.domain.product.entity;

import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_products_category"))
    private Category category;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "price", nullable = false)
    private long price;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status;

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "차감 수량은 1 이상이어야 합니다.");
        }

        if (this.status != ProductStatus.ON_SALE) {
            throw new IllegalStateException("판매 중인 상품만 주문할 수 있습니다.");
        }

        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("상품 재고가 부족합니다.");
        }

        this.stockQuantity -= quantity;

        if (this.stockQuantity == 0) {this.status = ProductStatus.SOLD_OUT;}
    }

    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "복구 수량은 1 이상이어야 합니다.");
        }

        this.stockQuantity = Math.addExact(this.stockQuantity, quantity);

        if (this.status == ProductStatus.SOLD_OUT) {
            this.status = ProductStatus.ON_SALE;
        }
    }
}
