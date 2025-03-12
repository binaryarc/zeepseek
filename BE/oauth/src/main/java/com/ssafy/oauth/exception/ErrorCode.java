package com.ssafy.oauth.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 공통 오류
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "E001", "유효하지 않은 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E002", "서버 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E003", "요청한 리소스를 찾을 수 없습니다."),

    // 인증 관련 오류
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다."),
    TOKEN_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "A004", "토큰 생성에 실패했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "유효하지 않은 리프레시 토큰입니다."),

    // 사용자 관련 오류
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "이미 사용 중인 닉네임입니다."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U003", "소셜 로그인에 실패했습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "U004", "인증에 실패했습니다."),

    // 매물 관련 오류
    PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "매물을 찾을 수 없습니다."),
    NOT_PROPERTY_OWNER(HttpStatus.FORBIDDEN, "P002", "해당 매물의 소유자가 아닙니다."),

    // 지역(동) 관련 오류
    DONG_NOT_FOUND(HttpStatus.NOT_FOUND, "D001", "해당 동을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}