package com.example.coffee.domain.product.popular.controller;

import com.example.coffee.domain.product.popular.dto.PopularMenuResponse;
import com.example.coffee.domain.product.popular.service.PopularMenuService;
import com.example.coffee.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/popular")
public class PopularMenuController {

    private final PopularMenuService popularMenuService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PopularMenuResponse>>> getPopularMenus() {
        return ResponseEntity.ok(ApiResponse.ok(popularMenuService.getPopularMenus()));
    }
}