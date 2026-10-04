package com.example.coffee.domain.auth.controller;

import com.example.coffee.domain.auth.dto.request.LoginRequest;
import com.example.coffee.domain.auth.dto.request.SignupRequest;
import com.example.coffee.domain.auth.dto.response.TokenResponse;
import com.example.coffee.domain.auth.service.AuthService;
import com.example.coffee.global.error.BusinessException;
import com.example.coffee.global.error.ErrorCode;
import com.example.coffee.global.jwt.JwtProvider;
import com.example.coffee.global.response.ApiResponse;
import com.example.coffee.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;
    private final JwtProvider jwtProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response) {
        AuthService.TokenAndRefresh result = authService.login(request);
        cookieUtil.addRefreshTokenCookie(response, result.refreshToken(), jwtProvider.getRefreshExpiration());
        return ResponseEntity.ok(ApiResponse.ok(result.tokenResponse()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(
            @CookieValue(name = CookieUtil.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        try {
            AuthService.TokenAndRefresh result = authService.reissue(refreshToken);
            cookieUtil.addRefreshTokenCookie(response, result.refreshToken(), jwtProvider.getRefreshExpiration());
            return ResponseEntity.ok(ApiResponse.ok(result.tokenResponse()));
        } catch (BusinessException e) {
            // 동시 재발급에서 진 요청은 쿠키를 건드리지 않음 (이긴 요청이 설정한 새 쿠키 보호)
            if (e.getErrorCode() != ErrorCode.REFRESH_TOKEN_ALREADY_ROTATED) {
                cookieUtil.deleteRefreshTokenCookie(response);
            }
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = CookieUtil.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        cookieUtil.deleteRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}