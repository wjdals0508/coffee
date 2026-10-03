package com.example.coffee.domain.point.dto.response;

import com.example.coffee.domain.point.entity.UserPoint;

public record PointBalanceResponse(
        Long userId,
        long balance
) {
    public static PointBalanceResponse from(UserPoint userPoint) {
        return new PointBalanceResponse(userPoint.getUserId(), userPoint.getBalance());
    }
}