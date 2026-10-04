package com.example.coffee.domain.cart.dto.response;

import com.example.coffee.domain.cart.entity.CartItem;
import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.entity.ProductStatus;

public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        long price,
        int quantity,
        long amount,
        ProductStatus status,
        boolean purchasable
) {
    public static CartItemResponse from(CartItem cartItem) {
        Product product = cartItem.getProduct();
        return new CartItemResponse(
                cartItem.getId(),
                product.getId(),
                product.getName(),
                product.getPrice(),
                cartItem.getQuantity(),
                Math.multiplyExact(product.getPrice(), cartItem.getQuantity()),
                product.getStatus(),
                cartItem.isPurchasable()
        );
    }
}