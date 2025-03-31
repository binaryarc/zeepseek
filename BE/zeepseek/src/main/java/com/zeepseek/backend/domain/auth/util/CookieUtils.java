package com.zeepseek.backend.domain.auth.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeepseek.backend.domain.user.dto.UserDto;
import org.springframework.util.SerializationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

public class CookieUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_MAX_AGE = 7 * 24 * 60 * 60; // 7일(초 단위)
    private static final String USER_COOKIE_NAME = "user_info";
    private static final String USER_ID_COOKIE_NAME = "user_id";

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }

        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(name.equals(USER_COOKIE_NAME)) // user_info는 httpOnly 설정
                .secure(true)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue())
        ));
    }

    /**
     * UserDto 객체와 리프레시 토큰을 user_info 쿠키에 함께 저장
     */
    public static void addUserCookie(HttpServletResponse response, UserDto userDto, String refreshToken) {
        try {
            // UserDto 객체에 리프레시 토큰 필드 추가 (임시 필드)
            userDto.setRefreshToken(refreshToken);

            // UserDto를 JSON 문자열로 변환
            String userJson = objectMapper.writeValueAsString(userDto);

            // JSON 문자열을 Base64로 인코딩 (쿠키 문자 제한 회피)
            String encodedUserInfo = Base64.getEncoder().encodeToString(userJson.getBytes(StandardCharsets.UTF_8));

            // 1. 전체 사용자 정보 쿠키 설정 (refreshToken 포함)
            ResponseCookie userCookie = ResponseCookie.from(USER_COOKIE_NAME, encodedUserInfo)
                    .path("/")
                    .maxAge(DEFAULT_MAX_AGE)
                    .httpOnly(true) // HTTP Only로 설정 (리프레시 토큰 보안)
                    .secure(true) // HTTPS에서만 전송되도록 설정
                    .sameSite("None") // 크로스 사이트 요청을 위해 None으로 설정
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, userCookie.toString());

            // 2. 사용자 ID 쿠키 설정 (빠른 접근용)
            ResponseCookie userIdCookie = ResponseCookie.from(USER_ID_COOKIE_NAME, userDto.getIdx().toString())
                    .path("/")
                    .maxAge(DEFAULT_MAX_AGE)
                    .httpOnly(false) // JavaScript에서 접근 가능하도록 설정
                    .secure(true) // HTTPS에서만 전송되도록 설정
                    .sameSite("None") // 크로스 사이트 요청을 위해 None으로 설정
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, userIdCookie.toString());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("사용자 정보를 쿠키로 변환하는 중 오류 발생", e);
        }
    }

    /**
     * 요청에서 사용자 정보 쿠키 읽기
     */
    public static Optional<UserDto> getUserFromCookie(HttpServletRequest request) {
        Optional<Cookie> cookieOpt = getCookie(request, USER_COOKIE_NAME);

        if (cookieOpt.isPresent()) {
            Cookie cookie = cookieOpt.get();
            try {
                // Base64 디코딩
                byte[] decodedBytes = Base64.getDecoder().decode(cookie.getValue());
                String userJson = new String(decodedBytes, StandardCharsets.UTF_8);

                // JSON을 UserDto로 변환
                UserDto userDto = objectMapper.readValue(userJson, UserDto.class);
                return Optional.of(userDto);
            } catch (Exception e) {
                // 쿠키 파싱 실패
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * user_info 쿠키에서 리프레시 토큰 추출
     */
    public static Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        Optional<UserDto> userOpt = getUserFromCookie(request);
        return userOpt.map(UserDto::getRefreshToken);
    }
}