package com.example.coffee.global.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_PATH = "/api/auth";

    private final boolean secure;
    private final String sameSite;

    public CookieUtil(@Value("${auth.cookie.secure}") boolean secure,
                      @Value("${auth.cookie.same-site}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeMillis) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                buildCookie(refreshToken, Duration.ofMillis(maxAgeMillis)).toString());
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString());
    }

    private ResponseCookie buildCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(REFRESH_TOKEN_PATH)
                .maxAge(maxAge)
                .build();
    }
}