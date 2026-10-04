package com.example.coffee.domain.cart.service;

import com.example.coffee.domain.cart.dto.request.CartItemAddRequest;
import com.example.coffee.domain.cart.dto.request.CartItemUpdateRequest;
import com.example.coffee.domain.cart.dto.response.CartItemResponse;
import com.example.coffee.domain.cart.entity.CartItem;
import com.example.coffee.domain.cart.repository.CartItemRepository;
import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.repository.ProductRepository;
import com.example.coffee.domain.user.entity.User;
import com.example.coffee.domain.user.repository.UserRepository;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CartItemResponse addItem(Long userId, CartItemAddRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(userId, product.getId())
                .map(existingItem -> {
                    existingItem.addQuantity(request.quantity());
                    return existingItem;
                })
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    return cartItemRepository.save(CartItem.of(user, product, request.quantity()));
                });

        return CartItemResponse.from(cartItem);
    }

    public List<CartItemResponse> getItems(Long userId) {
        return cartItemRepository.findAllByUserIdOrderByCreatedAtAscIdAsc(userId).stream()
                .map(CartItemResponse::from)
                .toList();
    }

    @Transactional
    public CartItemResponse updateItemQuantity(Long userId, Long cartItemId, CartItemUpdateRequest request) {
        CartItem cartItem = cartItemRepository.findByIdAndUserId(cartItemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItem.changeQuantity(request.quantity());
        return CartItemResponse.from(cartItem);
    }

    @Transactional
    public void deleteItem(Long userId, Long cartItemId) {
        cartItemRepository.deleteAllByUserIdAndIdIn(userId, List.of(cartItemId));
    }

    @Transactional
    public void deleteItems(Long userId, List<Long> cartItemIds) {
        cartItemRepository.deleteAllByUserIdAndIdIn(userId, cartItemIds);
    }
}