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

    private static final int MAX_QUANTITY_PER_ITEM = 99;

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

    @Column(name = "stock", nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    /** 구매 가능 여부 검증 (장바구니 담기, 주문 항목 생성 시) */
    public void validatePurchasable(int quantity) {
        if (this.status != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ON_SALE);
        }
        if (quantity < 1 || quantity > MAX_QUANTITY_PER_ITEM) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY,
                    "상품당 수량은 1개 이상 " + MAX_QUANTITY_PER_ITEM + "개 이하여야 합니다.");
        }
        if (this.stock < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "'" + this.name + "'의 재고가 부족합니다. (남은 수량: " + this.stock + ")");
        }
    }

    /** 재고 차감 — 반드시 락을 잡은 상태에서 호출 */
    public void decreaseStock(int quantity) {
        validatePurchasable(quantity);

        this.stock -= quantity;
        if (this.stock == 0) {
            this.status = ProductStatus.SOLD_OUT;
        }
    }

    /** 재고 복구 — 주문 취소 시 */
    public void restoreStock(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY, "복구 수량은 1 이상이어야 합니다.");
        }

        boolean soldOutByStock = this.status == ProductStatus.SOLD_OUT && this.stock == 0;

        this.stock = Math.addExact(this.stock, quantity);
        if (soldOutByStock) {
            this.status = ProductStatus.ON_SALE;
        }
    }
}