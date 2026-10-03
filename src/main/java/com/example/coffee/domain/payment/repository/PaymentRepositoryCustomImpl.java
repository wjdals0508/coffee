package com.example.coffee.domain.payment.repository;

import com.example.coffee.domain.payment.entity.Payment;
import com.example.coffee.domain.payment.entity.QPayment;
import com.example.coffee.domain.user.entity.QUser;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;


@RequiredArgsConstructor
public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QPayment payment = QPayment.payment;
    private final QUser user = QUser.user;

    // 결제 조회
    @Override
    public Optional<Payment> findPaymentByUserId(Long userId) {

        Payment result = queryFactory
                .selectFrom(payment)
                .where(payment.user.id.eq(userId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    // 결제 리스트 조회
    @Override
    public Page<Payment> findPaymentsByUserId(Long userId, Pageable pageable) {
        List<Payment> payments = queryFactory
                .selectFrom(payment)
                .where(payment.user.id.eq(userId))
                .orderBy(payment.createdAt.desc(), payment.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(payment.count())
                .from(payment)
                .where(payment.user.id.eq(userId));

        return PageableExecutionUtils.getPage(
                payments,
                pageable,
                () -> {
                    Long count = countQuery.fetchOne();
                    return count == null ? 0L : count;
                }
        );
    }

    // Webhook에서 받아온 portonePaymentId 조건으로 Payment 조회 시 연관된 Order를 fetch join 으로 함께 로딩
    @Override
    public Optional<Payment> findByPortonePaymentId(String portonePaymentId) {
        Payment result = queryFactory
                .selectFrom(payment)
                .where(payment.portonePaymentId.eq(portonePaymentId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}




