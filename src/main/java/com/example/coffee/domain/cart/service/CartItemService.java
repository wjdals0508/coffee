package com.example.coffee.domain.cart.service;

import com.example.coffee.domain.cart.dto.request.CartItemAddRequest;
import com.example.coffee.domain.cart.dto.request.CartItemUpdateRequest;
import com.example.coffee.domain.cart.dto.response.CartItemResponse;
import com.example.coffee.domain.cart.entity.CartItem;
import com.example.coffee.domain.cart.repository.CartItemRepository;
import com.example.coffee.domain.product.entity.Product;
import com.example.coffee.domain.product.repository.ProductRepository;
import com.example.coffee.domain.user.entiry.User;
import com.example.coffee.domain.user.repository.UserRepository;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartItemService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // 상품 추가
    @Transactional
    public CartItemResponse addItem(Long userId, CartItemAddRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        CartItem cartItem = cartItemRepository
                .findByUser_IdAndProduct_Id(userId, product.getId())
                .map(existingItem -> {
                    existingItem.addQuantity(request.quantity());
                    return existingItem;
                })
                .orElseGet(() -> CartItem.of(user, product, request.quantity()));

        return CartItemResponse.from(cartItemRepository.save(cartItem));
    }

    // 상품 리스트 (조회)
    @Transactional(readOnly = true)
    public List<CartItemResponse> getItems(Long userId) {
        return cartItemRepository.findAllByUser_IdOrderByCreatedAtAsc(userId).stream()
                .map(CartItemResponse::from)
                .toList();
    }

    // 상품 리스트 (주문)
    @Transactional(readOnly = true)
    public List<CartItemResponse> getItems(Long userId, List<Long> cartIds) {
        List<Long> requestedIds = getDistinctIds(cartIds);
        if (requestedIds.isEmpty()) {
            return List.of();
        }

        List<CartItem> cartItems = getCartItems(userId, requestedIds);
        Map<Long, CartItem> cartItemById = cartItems.stream()
                .collect(Collectors.toMap(CartItem::getId, Function.identity()));

        return requestedIds.stream()
                .map(cartItemById::get)
                .map(CartItemResponse::from)
                .toList();
    }

    @Transactional
    public CartItemResponse updateItemQuantity(
            Long userId,
            Long cartItemId,
            CartItemUpdateRequest request
    ) {
        CartItem cartItem = getCartItem(userId, cartItemId);
        cartItem.changeQuantity(request.quantity());

        return CartItemResponse.from(cartItem);
    }

    @Transactional
    public void deleteItem(Long memberId, Long cartItemId) {
        CartItem cartItem = getCartItem(memberId, cartItemId);
        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public void deleteItems(Long memberId, List<Long> cartItemIds) {
        List<Long> requestedIds = getDistinctIds(cartItemIds);
        if (requestedIds.isEmpty()) {
            return;
        }

        List<CartItem> cartItems = getCartItems(memberId, requestedIds);
        cartItemRepository.deleteAllInBatch(cartItems);
    }

    private CartItem getCartItem(Long userId, Long cartItemId) {
        return cartItemRepository.findByIdAndUser_Id(cartItemId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    private List<CartItem> getCartItems(Long userId, List<Long> cartItemIds) {
        List<CartItem> cartItems = cartItemRepository.findAllByUser_IdAndIdIn(userId, cartItemIds);
        if (cartItems.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return cartItems;
    }

    private List<Long> getDistinctIds(List<Long> ids) {
        if (ids == null || ids.stream().anyMatch(Objects::isNull)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "장바구니 상품 ID는 필수입니다.");
        }
        return ids.stream().distinct().toList();
    }




}
