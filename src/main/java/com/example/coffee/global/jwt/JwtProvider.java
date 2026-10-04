package com.example.coffee.global.jwt;

import com.example.coffee.domain.user.entity.Role;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_SESSION_ID = "sid";

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    // ===== 발급 =====

    public String createAccessToken(Long userId, Role role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId, String sessionId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_SESSION_ID, sessionId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpiration))
                .signWith(secretKey)
                .compact();
    }

    // ===== 검증·파싱 =====

    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = parseClaims(token, ErrorCode.ACCESS_TOKEN_EXPIRED, ErrorCode.INVALID_TOKEN);
        validateType(claims, TYPE_ACCESS, ErrorCode.INVALID_TOKEN);

        try {
            return new AccessTokenClaims(
                    Long.valueOf(claims.getSubject()),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class))
            );
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    public RefreshTokenClaims parseRefreshToken(String token) {
        Claims claims = parseClaims(token, ErrorCode.REFRESH_TOKEN_EXPIRED, ErrorCode.INVALID_REFRESH_TOKEN);
        validateType(claims, TYPE_REFRESH, ErrorCode.INVALID_REFRESH_TOKEN);

        String sessionId = claims.get(CLAIM_SESSION_ID, String.class);
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            return new RefreshTokenClaims(Long.valueOf(claims.getSubject()), sessionId);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    public long getAccessExpiration() {
        return accessExpiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    // ===== 내부 =====

    private Claims parseClaims(String token, ErrorCode expiredCode, ErrorCode invalidCode) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(expiredCode);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(invalidCode);
        }
    }

    private void validateType(Claims claims, String expectedType, ErrorCode invalidCode) {
        if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
            throw new BusinessException(invalidCode);
        }
    }
}