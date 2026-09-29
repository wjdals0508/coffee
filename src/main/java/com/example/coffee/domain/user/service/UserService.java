package com.example.coffee.domain.user.service;

import com.example.coffee.domain.user.dto.response.GetUserResponse;
import com.example.coffee.domain.user.entiry.User;
import com.example.coffee.domain.user.repository.UserRepository;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public GetUserResponse getMe(Long memberId) {

        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return GetUserResponse.from(user);
    }

}
