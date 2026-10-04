package com.example.coffee.domain.point.controller;

import com.example.coffee.domain.point.dto.response.PointBalanceResponse;
import com.example.coffee.domain.point.dto.response.PointHistoryResponse;
import com.example.coffee.domain.point.service.PointService;
import com.example.coffee.global.response.ApiResponse;
import com.example.coffee.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @GetMapping
    public ResponseEntity<ApiResponse<PointBalanceResponse>> getBalance(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pointService.getBalance(userId)));
    }

    @GetMapping("/histories")
    public ResponseEntity<ApiResponse<PageResponse<PointHistoryResponse>>> getHistories(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(pointService.getHistories(userId, pageable))));
    }
}