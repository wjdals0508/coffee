package com.example.coffee.domain.payment.gateway;

import java.time.LocalDateTime;

public record PaymentApproval(
        String pgTransactionId,
        LocalDateTime approvedAt
) {}
