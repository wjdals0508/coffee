package com.example.coffee.domain.cart.dto.response;

import com.example.coffee.domain.cart.entity.CartItem;

public record CartItemResponse(
        Long id,
        Long productId,
        int quantity
) {
    public static CartItemResponse from(CartItem cartItem) {
        return new CartItemResponse(
                cartItem.getId(),
                cartItem.getProduct().getId(),
                cartItem.getQuantity()
        );
    }
}
