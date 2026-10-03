package com.example.coffee.domain.payment.service;

import com.example.coffee.domain.payment.entity.FailReason;
import com.example.coffee.domain.payment.entity.Payment;
import com.example.coffee.domain.payment.repository.PaymentRepository;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;

    // 결제 생성
    @Transactional
    public Payment createPayment(Long amount) {
        Payment payment = Payment.builder()
                .amount(amount)
                .build();
        return paymentRepository.save(payment);
    }

    // 결제 완료
    @Transactional
    public void completePayment(Payment payment) {
        payment.complete();
    }

    // 결제 실패 (상세 사유 지정)
    @Transactional
    public void failPayment(Payment payment, FailReason reason) {
        payment.fail(reason);
    }

    // 결제 상태 변경(Canceled)
    @Transactional
    public void cancelPayment(Payment payment) {
        payment.cancel();
    }

    // 결제 조회
    public Payment findByIdWithOrder(Long userId) {
        return paymentRepository.findPaymentByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }
}
