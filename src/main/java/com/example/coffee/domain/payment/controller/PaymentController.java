package com.example.coffee.domain.payment.controller;

import com.example.coffee.domain.payment.dto.response.PaymentResponse;
import com.example.coffee.domain.payment.facade.PaymentChargeFacade;
import com.example.coffee.domain.payment.service.PaymentService;
import com.example.coffee.domain.point.dto.request.PointChargeRequest;
import com.example.coffee.domain.point.dto.response.PointChargeResponse;
import com.example.coffee.global.response.ApiResponse;
import com.example.coffee.global.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final PaymentChargeFacade paymentChargeFacade;
    private final PaymentService paymentService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<PointChargeResponse>> charge(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) @NotBlank @Size(max = 64) String idempotencyKey,
            @Valid @RequestBody PointChargeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(paymentChargeFacade.charge(idempotencyKey, userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(paymentService.getMyPayments(userId, pageable))));
    }
}