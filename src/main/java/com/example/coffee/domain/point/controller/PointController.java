package com.example.coffee.domain.point.controller;

import com.example.coffee.domain.point.dto.request.PointChargeRequest;
import com.example.coffee.domain.point.dto.response.PointBalanceResponse;
import com.example.coffee.domain.point.dto.response.PointChargeResponse;
import com.example.coffee.domain.point.dto.response.PointHistoryResponse;
import com.example.coffee.domain.point.facade.PointChargeFacade;
import com.example.coffee.domain.point.service.PointService;
import com.example.coffee.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final PointChargeFacade pointChargeFacade;
    private final PointService pointService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<PointChargeResponse>> charge(
            @AuthenticationPrincipal Long memberId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) @NotBlank @Size(max = 64) String idempotencyKey,
            @Valid @RequestBody PointChargeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pointChargeFacade.charge(idempotencyKey, memberId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PointBalanceResponse>> getBalance(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pointService.getBalance(memberId)));
    }

    @GetMapping("/histories")
    public ResponseEntity<ApiResponse<PagedModel<PointHistoryResponse>>> getHistories(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(new PagedModel<>(pointService.getHistories(memberId, pageable))));
    }
}