package com.example.coffee.domain.payment.repository;

import com.example.coffee.domain.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;


public interface  PaymentRepositoryCustom {

    Optional<Payment> findPaymentByUserId(Long userId);

    Page<Payment> findPaymentsByUserId(Long userId, Pageable pageable);

    Optional<Payment> findByPortonePaymentId(String portonePaymentId);
}