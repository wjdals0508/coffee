package com.example.coffee.domain.payment.repository;

import com.example.coffee.domain.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}