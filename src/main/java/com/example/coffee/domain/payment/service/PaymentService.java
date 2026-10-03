package com.example.coffee.domain.payment.service;

import com.example.coffee.domain.payment.dto.response.PaymentResponse;
import com.example.coffee.domain.payment.entity.Payment;
import com.example.coffee.domain.payment.gateway.PaymentGateway;
import com.example.coffee.domain.payment.repository.PaymentRepository;
import com.example.coffee.domain.point.dto.response.PointChargeResponse;
import com.example.coffee.domain.point.service.PointService;
import com.example.coffee.domain.user.entity.User;
import com.example.coffee.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final PointService pointService;

    @Transactional
    public PointChargeResponse chargePoint(Long userId, long amount) {
        User user = userRepository.getReferenceById(userId);

        Payment payment = paymentRepository.save(
                Payment.builder()
                        .user(user)
                        .amount(amount)
                        .build()
        );

        paymentGateway.approve(payment.getPortonePaymentId(), amount);
        payment.complete();   // PENDING → COMPLETED, paidAt 기록

        return pointService.charge(userId, amount);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Long memberId, Pageable pageable) {
        return paymentRepository.findPaymentsByUserId(memberId, pageable)
                .map(PaymentResponse::from);
    }
}