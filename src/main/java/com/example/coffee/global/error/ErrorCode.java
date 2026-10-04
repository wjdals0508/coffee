package com.example.coffee.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 에러가 발생했습니다."),
    REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "COMMON_003", "이미 처리 중인 요청입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_004", "요청한 리소스를 찾을 수 없습니다."),
    LOCK_TIMEOUT(HttpStatus.CONFLICT, "COMMON_005", "요청이 많아 처리하지 못했습니다. 잠시 후 다시 시도해 주세요."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "COMMON_006", "다른 요청과 충돌했습니다. 잠시 후 다시 시도해 주세요."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_002", "권한이 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_004", "액세스 토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005", "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_006", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_007", "로그인이 만료되었습니다. 다시 로그인해 주세요."),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "AUTH_008", "보안을 위해 로그아웃되었습니다. 다시 로그인해 주세요."),
    REFRESH_TOKEN_ALREADY_ROTATED(HttpStatus.CONFLICT, "AUTH_009", "다른 요청에서 이미 토큰을 재발급했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER_002", "이미 존재하는 이메일입니다."),

    // Point
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "POINT_001", "1회 최대 충전 금액을 초과했습니다."),
    NOT_ENOUGH_POINT(HttpStatus.BAD_REQUEST, "POINT_002", "포인트가 부족합니다."),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001", "상품을 찾을 수 없습니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "PRODUCT_002", "재고가 부족합니다."),
    PRODUCT_NOT_ON_SALE(HttpStatus.CONFLICT, "PRODUCT_003", "현재 판매하지 않는 상품입니다."),

    // Cart
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_001", "장바구니 상품을 찾을 수 없습니다."),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_001", "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "ORDER_002", "유효하지 않은 주문 상태 변경입니다."),
    ORDER_NOT_CANCELABLE(HttpStatus.CONFLICT, "ORDER_003", "취소할 수 없는 주문입니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER_004", "주문 수량은 1 이상이어야 합니다."),
    ORDER_ALREADY_CANCELED(HttpStatus.CONFLICT, "ORDER_005", "이미 취소된 주문입니다."),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_001", "결제 정보를 찾을 수 없습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "PAYMENT_002", "유효하지 않은 결제 상태 변경입니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}