package com.example.coffee.global.jwt;

import com.example.coffee.domain.user.entity.Role;

public record AccessTokenClaims(Long userId, Role role) {}