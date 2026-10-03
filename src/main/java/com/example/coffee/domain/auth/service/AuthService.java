package com.example.coffee.domain.auth.service;


import com.example.coffee.domain.auth.dto.request.LoginRequest;
import com.example.coffee.domain.auth.dto.response.TokenResponse;
import com.example.coffee.domain.auth.entity.RefreshToken;
import com.example.coffee.domain.auth.repository.RefreshTokenRepository;
import com.example.coffee.domain.user.entity.User;
import com.example.coffee.domain.user.repository.UserRepository;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import com.example.coffee.global.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public TokenAndRefresh login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        saveOrUpdateRefreshToken(user.getId(), refreshToken);

        return new TokenAndRefresh(new TokenResponse(accessToken), refreshToken);
    }

    @Transactional
    public TokenAndRefresh reissue(String refreshToken) {
        if (refreshToken == null || !jwtProvider.validate(refreshToken) || !jwtProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);

        RefreshToken saved = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (saved.isExpired() || !saved.matches(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getId());

        saved.update(newRefreshToken, LocalDateTime.now().plusSeconds(jwtProvider.getRefreshExpiration() / 1000));

        return new TokenAndRefresh(new TokenResponse(newAccessToken), newRefreshToken);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private void saveOrUpdateRefreshToken(Long userId, String refreshToken) {
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshExpiration() / 1000);

        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.update(refreshToken, expiredAt),
                        () -> refreshTokenRepository.save(RefreshToken.builder()
                                .userId(userId)
                                .token(refreshToken)
                                .expiredAt(expiredAt)
                                .build())
                );
    }

    public record TokenAndRefresh(TokenResponse tokenResponse, String refreshToken) {
    }
}