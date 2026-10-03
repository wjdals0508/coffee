package com.example.coffee.domain.payment.controller;

import com.example.coffee.domain.payment.dto.response.PaymentResponse;
import com.example.coffee.domain.payment.service.PaymentService;
import com.example.coffee.domain.point.dto.request.PointChargeRequest;
import com.example.coffee.domain.point.dto.response.PointChargeResponse;
import com.example.coffee.domain.point.facade.PointChargeFacade;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final PointChargeFacade pointChargeFacade;
    private final PaymentService paymentService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<PointChargeResponse>> charge(
            @AuthenticationPrincipal Long memberId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) @NotBlank @Size(max = 64) String idempotencyKey,
            @Valid @RequestBody PointChargeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(pointChargeFacade.charge(idempotencyKey, memberId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedModel<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(new PagedModel<>(paymentService.getMyPayments(memberId, pageable))));
    }
}