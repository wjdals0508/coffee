package com.example.coffee.domain.point.dto.response;

import com.example.coffee.domain.point.entity.PointHistory;
import com.example.coffee.domain.point.entity.PointType;

import java.time.LocalDateTime;

public record PointHistoryResponse(
        Long historyId,
        PointType type,
        long amount,
        long balanceAfter,
        Long orderId,
        LocalDateTime createdAt
) {
    public static PointHistoryResponse from(PointHistory history) {
        return new PointHistoryResponse(
                history.getId(),
                history.getType(),
                history.getAmount(),
                history.getBalanceAfter(),
                history.getOrder() != null ? history.getOrder().getId() : null,
                history.getCreatedAt()
        );
    }
}