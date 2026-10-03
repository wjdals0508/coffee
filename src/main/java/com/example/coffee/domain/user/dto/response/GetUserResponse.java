package com.example.coffee.domain.user.dto.response;

import com.example.coffee.domain.user.entity.User;

public record GetUserResponse(
        Long id,
        String email,
        String name
) {
    public static GetUserResponse from(User user) {

        return new GetUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }
}
