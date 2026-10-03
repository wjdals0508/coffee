package com.example.coffee.domain.cart.entity;

import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.user.entity.User;
import com.example.coffee.global.entity.BaseTimeEntity;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_items_user_product",
                columnNames = {"user_id", "product_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    private CartItem(User user, Product product, int quantity) {
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "회원은 필수입니다.");
        }
        if (product == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "상품은 필수입니다.");
        }
        validateQuantity(quantity);
        product.validatePurchasable(quantity);

        this.user = user;
        this.product = product;
        this.quantity = quantity;
    }

    public static CartItem of(User user, Product product, int quantity) {
        return new CartItem(user, product, quantity);
    }

    private static void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "장바구니 수량은 1 이상이어야 합니다.");
        }
    }

    public void addQuantity(int quantity) {
        validateQuantity(quantity);
        int totalQuantity = Math.addExact(this.quantity, quantity);
        this.product.validatePurchasable(totalQuantity);
        this.quantity = totalQuantity;
    }

    public void changeQuantity(int quantity) {
        validateQuantity(quantity);
        this.product.validatePurchasable(quantity);
        this.quantity = quantity;
    }
}
