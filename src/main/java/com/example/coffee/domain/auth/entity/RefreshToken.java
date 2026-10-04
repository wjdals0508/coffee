package com.example.coffee.domain.auth.entity;

import com.example.coffee.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_user", columnNames = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "previous_token_hash", length = 64)
    private String previousTokenHash;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    private RefreshToken(Long userId, String sessionId, String tokenHash, LocalDateTime expiredAt) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.expiredAt = expiredAt;
    }

    public static RefreshToken create(Long userId, String sessionId, String tokenHash, LocalDateTime expiredAt) {
        return new RefreshToken(userId, sessionId, tokenHash, expiredAt);
    }

    /** 직전 토큰이고, 교체된 지 유예 시간 이내인가 (동시 재발급 경쟁에서 진 요청 판별) */
    public boolean isRecentlyRotatedFrom(String presentedHash, LocalDateTime now, Duration gracePeriod) {
        return previousTokenHash != null
                && previousTokenHash.equals(presentedHash)
                && rotatedAt != null
                && rotatedAt.isAfter(now.minus(gracePeriod));
    }
}