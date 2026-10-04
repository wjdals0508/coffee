package com.example.coffee.global.jwt;

public record RefreshTokenClaims(Long userId, String sessionId) {}