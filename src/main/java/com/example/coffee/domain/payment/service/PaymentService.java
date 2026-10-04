package com.example.coffee.domain.payment.service;

import com.example.coffee.domain.payment.dto.response.PaymentResponse;
import com.example.coffee.domain.payment.entity.Payment;
import com.example.coffee.domain.payment.gateway.PaymentGateway;
import com.example.coffee.domain.payment.repository.PaymentRepository;
import com.example.coffee.domain.point.dto.response.PointChargeResponse;
import com.example.coffee.domain.point.service.PointService;
import com.example.coffee.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final PointService pointService;

    /** 결제 승인 후 포인트 충전 (결제 기록과 충전은 함께 성공하거나 함께 실패) */
    @Transactional
    public PointChargeResponse chargePoint(Long userId, long amount) {
        Payment payment = paymentRepository.save(
                Payment.create(userRepository.getReferenceById(userId), amount)
        );

        paymentGateway.approve(payment.getPaymentKey(), amount);
        payment.complete();

        return pointService.charge(userId, amount);
    }

    public Page<PaymentResponse> getMyPayments(Long userId, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
                .map(PaymentResponse::from);
    }
}