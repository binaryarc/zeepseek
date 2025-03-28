package com.zeepseek.backend.domain.auth.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeepseek.backend.domain.user.dto.UserDto;
import org.springframework.util.SerializationUtils;

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
    private static final String USER_IDX_COOKIE_NAME = "user_idx";

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
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
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
     * UserDto 객체를 쿠키로 저장
     */
    public static void addUserCookie(HttpServletResponse response, UserDto userDto) {
        try {
            // UserDto를 JSON 문자열로 변환
            String userJson = objectMapper.writeValueAsString(userDto);

            // JSON 문자열을 Base64로 인코딩 (쿠키 문자 제한 회피)
            String encodedUserInfo = Base64.getEncoder().encodeToString(userJson.getBytes(StandardCharsets.UTF_8));

            // 1. 전체 사용자 정보 쿠키 설정
            Cookie userCookie = new Cookie(USER_COOKIE_NAME, encodedUserInfo);
            userCookie.setPath("/");
            userCookie.setMaxAge(DEFAULT_MAX_AGE);
            userCookie.setHttpOnly(false); // JavaScript에서 접근 가능하도록 설정
            userCookie.setSecure(true); // HTTPS에서만 전송되도록 설정
            response.addCookie(userCookie);

            // 2. 사용자 ID 쿠키 설정 (빠른 접근용)
            Cookie idxCookie = new Cookie(USER_IDX_COOKIE_NAME, userDto.getIdx().toString());
            idxCookie.setPath("/");
            idxCookie.setMaxAge(DEFAULT_MAX_AGE);
            idxCookie.setHttpOnly(false); // JavaScript에서 접근 가능하도록 설정
            idxCookie.setSecure(true); // HTTPS에서만 전송되도록 설정
            response.addCookie(idxCookie);

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
}