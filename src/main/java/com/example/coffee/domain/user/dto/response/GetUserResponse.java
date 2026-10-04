package com.example.coffee.domain.user.dto.response;

import com.example.coffee.domain.user.entity.Role;
import com.example.coffee.domain.user.entity.User;

public record GetUserResponse(
        Long id,
        String email,
        String name,
        Role role
) {
    public static GetUserResponse from(User user) {

        return new GetUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
}
