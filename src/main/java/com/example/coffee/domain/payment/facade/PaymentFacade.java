package com.example.coffee.domain.payment.facade;

import com.example.coffee.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.coffee.domain.payment.dto.response.PaymentConfirmResponse;
import com.example.coffee.domain.payment.entity.FailReason;
import com.example.coffee.domain.payment.entity.Payment;
import com.example.coffee.domain.payment.entity.PaymentStatus;
import com.example.coffee.domain.payment.port.PaymentGateway;
import com.example.coffee.domain.payment.port.PaymentGatewayResponse;
import com.example.coffee.domain.payment.service.PaymentService;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    private static final String PG_STATUS_PAID = "PAID";

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;

    /**
     * 결제 승인
     */
    public PaymentConfirmResponse confirmPayment(Long memberId, PaymentConfirmRequest request) {

        Payment payment = paymentService.findByIdWithOrder(memberId);

        // 이미 처리된 결제인지 검증
        validatePaymentStatus(payment);

        // 요청의 PortOne Payment ID와 DB의 Payment ID가 같은지 검증
        validatePortonePaymentId(payment, request);

        // PortOne 결제 정보 조회
        PaymentGatewayResponse pgPayment = paymentGateway.getPayment(payment.getPortonePaymentId());

        // PG 결제 상태 검증
        if (!PG_STATUS_PAID.equals(pgPayment.status())) {
            log.error("결제 승인 실패 — PG 상태 비정상: paymentId={}, pgStatus={}", payment.getId(), pgPayment.status());

            paymentCommandService.failPaymentAndOrder(order.getId(), FailReason.PG_DECLINED);

            throw new BusinessException(ErrorCode.PAYMENT_NOT_PAID
            );
        }

        // 결제 금액 검증
        if (payment.getPgAmount() != pgPayment.totalAmount()) {
            log.error("결제 승인 실패 — 금액 불일치: paymentId={}, DB pgAmount={}, PG금액={}", payment.getId(), payment.getPgAmount(), pgPayment.totalAmount());

            try {
                paymentGateway.cancelPayment(payment.getPortonePaymentId(), "결제 금액 불일치 자동 취소", null);
            } catch (Exception e) {
                log.error("PG 자동 취소 실패 : 수동 처리 필요: portonePaymentId={}", payment.getPortonePaymentId(), e);
            }

            paymentCommandService.failPaymentAndOrder(order.getId(), FailReason.AMOUNT_MISMATCH);

            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 모든 외부 결제 검증이 끝났으므로
        // 실제 내부 결제 승인 처리를 CommandService에 위임
        return paymentCommandService.approvePaymentAndOrder(order.getId());
    }

     // 결제 상태 검증
    private void validatePaymentStatus(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.ALREADY_PROCESSED_PAYMENT
            );
        }
    }

     // PortOne Payment ID 검증
    private void validatePortonePaymentId(Payment payment, PaymentConfirmRequest request) {
        String portonePaymentId = payment.getPortonePaymentId();

        if (!portonePaymentId.equals(request.portonePaymentId())) {
            log.warn(
                    "결제 승인 거부 — portonePaymentId 불일치: DB={}, 요청={}",
                    portonePaymentId,
                    request.portonePaymentId()
            );

            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE
            );
        }
    }


}
