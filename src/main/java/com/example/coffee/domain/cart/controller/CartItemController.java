package com.example.coffee.domain.cart.controller;

import com.example.coffee.domain.cart.dto.request.CartItemAddRequest;
import com.example.coffee.domain.cart.dto.request.CartItemUpdateRequest;
import com.example.coffee.domain.cart.dto.request.CartItemsDeleteRequest;
import com.example.coffee.domain.cart.dto.response.CartItemResponse;
import com.example.coffee.domain.cart.service.CartItemService;
import com.example.coffee.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart/items")
@RequiredArgsConstructor
public class CartItemController {

    private final CartItemService cartItemService;

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemResponse>> addItem(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cartItemService.addItem(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getItems(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cartItemService.getItems(userId)));
    }

    @PatchMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateItemQuantity(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cartItemService.updateItemQuantity(userId, cartItemId, request)));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long cartItemId
    ) {
        cartItemService.deleteItem(userId, cartItemId);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteItems(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CartItemsDeleteRequest request
    ) {
        cartItemService.deleteItems(userId, request.cartItemIds());
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
