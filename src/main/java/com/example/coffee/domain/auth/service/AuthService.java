package com.example.coffee.domain.auth.service;

import com.example.coffee.domain.auth.dto.request.LoginRequest;
import com.example.coffee.domain.auth.dto.request.SignupRequest;
import com.example.coffee.domain.auth.dto.response.TokenResponse;
import com.example.coffee.domain.auth.entity.RefreshToken;
import com.example.coffee.domain.auth.repository.RefreshTokenRepository;
import com.example.coffee.domain.point.service.PointService;
import com.example.coffee.domain.user.entity.User;
import com.example.coffee.domain.user.repository.UserRepository;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import com.example.coffee.global.jwt.JwtProvider;
import com.example.coffee.global.jwt.RefreshTokenClaims;
import com.example.coffee.global.jwt.TokenHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration REISSUE_GRACE_PERIOD = Duration.ofSeconds(10);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PointService pointService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // ===== 회원가입 =====

    @Transactional
    public void signup(SignupRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 중복 확인과 저장 사이에 같은 이메일로 다른 가입이 먼저 저장된 경우
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        pointService.createWallet(user);
    }

    // ===== 로그인 =====

    public TokenAndRefresh login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        LocalDateTime now = LocalDateTime.now();
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), sessionId);
        startSession(user.getId(), sessionId, TokenHasher.sha256(refreshToken), now);

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        return new TokenAndRefresh(new TokenResponse(accessToken), refreshToken);
    }

    // ===== 재발급 =====

    public TokenAndRefresh reissue(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshTokenClaims claims = jwtProvider.parseRefreshToken(refreshToken);
        String presentedHash = TokenHasher.sha256(refreshToken);

        LocalDateTime now = LocalDateTime.now();
        String newRefreshToken = jwtProvider.createRefreshToken(claims.userId(), claims.sessionId());

        int rotated = refreshTokenRepository.rotate(
                claims.userId(),
                claims.sessionId(),
                presentedHash,
                TokenHasher.sha256(newRefreshToken),
                now,
                refreshExpiredAt(now)
        );

        if (rotated == 0) {
            throw handleRotationFailure(claims, presentedHash, now);
        }

        // 권한은 DB에서 최신 값을 읽어 반영 (권한 변경이 재발급 시점에 적용되도록)
        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());
        return new TokenAndRefresh(new TokenResponse(newAccessToken), newRefreshToken);
    }

    // ===== 로그아웃 =====

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            RefreshTokenClaims claims = jwtProvider.parseRefreshToken(refreshToken);
            refreshTokenRepository.deleteCurrentSession(
                    claims.userId(), claims.sessionId(), TokenHasher.sha256(refreshToken));
        } catch (BusinessException e) {
            // 만료·위조된 토큰이어도 로그아웃은 성공으로 처리 (쿠키 삭제는 컨트롤러에서)
        }
    }

    // ===== 내부 메서드 =====

    /**
     * 교체 실패 원인 판별
     * 1. 세션 없음          → 로그아웃했거나 이미 폐기됨
     * 2. 세션 ID 다름       → 다른 기기 로그인에 밀려남 (탈취 아님)
     * 3. 직전 토큰 + 유예 내 → 동시 재발급 경쟁에서 진 요청 (탈취 아님)
     * 4. 그 외              → 이미 교체된 토큰의 재사용 = 탈취 의심 → 세션 폐기
     */
    private BusinessException handleRotationFailure(RefreshTokenClaims claims, String presentedHash, LocalDateTime now) {
        Optional<RefreshToken> current = refreshTokenRepository.findByUserId(claims.userId());

        if (current.isEmpty()) {
            return new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken saved = current.get();

        if (!saved.getSessionId().equals(claims.sessionId())) {
            return new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (saved.isRecentlyRotatedFrom(presentedHash, now, REISSUE_GRACE_PERIOD)) {
            return new BusinessException(ErrorCode.REFRESH_TOKEN_ALREADY_ROTATED);
        }

        refreshTokenRepository.revokeSession(claims.userId(), claims.sessionId());
        log.warn("리프레시 토큰 재사용 탐지 — 세션 폐기: userId={}, sessionId={}",
                claims.userId(), claims.sessionId());
        return new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED);
    }

    private void startSession(Long userId, String sessionId, String tokenHash, LocalDateTime now) {
        LocalDateTime expiredAt = refreshExpiredAt(now);

        if (refreshTokenRepository.replaceSession(userId, sessionId, tokenHash, now, expiredAt) > 0) {
            return;
        }
        try {
            refreshTokenRepository.save(RefreshToken.create(userId, sessionId, tokenHash, expiredAt));
        } catch (DataIntegrityViolationException e) {
            // 같은 사용자의 첫 로그인이 동시에 들어와 다른 요청이 먼저 INSERT한 경우
            refreshTokenRepository.replaceSession(userId, sessionId, tokenHash, now, expiredAt);
        }
    }

    private LocalDateTime refreshExpiredAt(LocalDateTime now) {
        return now.plus(Duration.ofMillis(jwtProvider.getRefreshExpiration()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record TokenAndRefresh(TokenResponse tokenResponse, String refreshToken) {
    }
}