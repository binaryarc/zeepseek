package com.zeepseek.backend.domain.auth.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeepseek.backend.domain.user.dto.UserDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.SerializationUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CookieUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_MAX_AGE = 7 * 24 * 60 * 60; // 7일(초 단위)
    private static final String USER_COOKIE_NAME = "user_info";
    private static final String USER_ID_COOKIE_NAME = "userId";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshtoken"; // 이름 변경
    private static final String USER_ACTIVITY_INFO_COOKIE_NAME = "user_activity_info"; // 사용자 활동 정보용 쿠키 이름

    /**
     * 쿠키 가져오기
     */
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

    /**
     * 쿠키 삭제
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(name.equals(REFRESH_TOKEN_COOKIE_NAME)) // refreshtoken만 httpOnly 설정
                .secure(true)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 모든 인증 관련 쿠키 삭제
     */
    public static void deleteAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, USER_COOKIE_NAME);
        deleteCookie(request, response, USER_ID_COOKIE_NAME);
        deleteCookie(request, response, REFRESH_TOKEN_COOKIE_NAME);
    }

    /**
     * UserDto 객체를 user_info 쿠키에 저장
     */
    public static void addUserInfoCookie(HttpServletResponse response, UserDto userDto) {
        try {
            // UserDto를 JSON 문자열로 변환
            String userJson = objectMapper.writeValueAsString(userDto);

            // JSON 문자열을 Base64로 인코딩 (쿠키 문자 제한 회피)
            String encodedUserInfo = Base64.getEncoder().encodeToString(userJson.getBytes(StandardCharsets.UTF_8));

            // user_info 쿠키 설정
            ResponseCookie userCookie = ResponseCookie.from(USER_COOKIE_NAME, encodedUserInfo)
                    .path("/")
                    .maxAge(DEFAULT_MAX_AGE)
                    .httpOnly(true) // JavaScript에서 접근 가능하도록 설정
                    .secure(true) // HTTPS에서만 전송되도록 설정
                    .sameSite("None")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, userCookie.toString());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("사용자 정보를 쿠키로 변환하는 중 오류 발생", e);
        }
    }

    /**
     * 사용자 ID를 user_id 쿠키에 저장
     */
    public static void addUserIdCookie(HttpServletResponse response, Integer userId) {
        if (userId != null) {
            ResponseCookie userIdCookie = ResponseCookie.from(USER_ID_COOKIE_NAME, userId.toString())
                    .path("/")
                    .maxAge(DEFAULT_MAX_AGE)
                    .httpOnly(true) // JavaScript에서 접근 가능하도록 설정
                    .secure(true) // HTTPS에서만 전송되도록 설정
                    .sameSite("None")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, userIdCookie.toString());
        }
    }

    /**
     * 리프레시 토큰을 refreshtoken 쿠키에 저장
     */
    public static void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            ResponseCookie refreshTokenCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                    .httpOnly(true) // JavaScript에서 접근 불가능하도록 설정
                    .secure(true) // HTTPS에서만 전송되도록 설정
                    .path("/" +
                            "")
                    .maxAge(DEFAULT_MAX_AGE)
                    .sameSite("None")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
        }
    }

    /**
     * 사용자 정보 한 번에 설정 - 네 가지 쿠키 모두 설정 (편의 메서드)
     */
    public static void addAllUserCookies(HttpServletResponse response, UserDto userDto, String refreshToken) {
        // 1. 사용자 정보 쿠키
        addUserInfoCookie(response, userDto);

        // 2. 사용자 ID 쿠키
        addUserIdCookie(response, userDto.getIdx());

        // 3. 리프레시 토큰 쿠키
        addRefreshTokenCookie(response, refreshToken);

        // 4. 사용자 활동 정보(나이, 성별) 쿠키 추가
        addUserActivityInfoCookie(response, userDto);

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
     * 리프레시 토큰 쿠키에서 값 가져오기
     */
    public static Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        Optional<Cookie> cookieOpt = getCookie(request, REFRESH_TOKEN_COOKIE_NAME);
        return cookieOpt.map(Cookie::getValue);
    }
    
    /**
     * 사용자 활동 정보(나이, 성별)를 user_activity_info 쿠키로 저장
     */
    public static void addUserActivityInfoCookie(HttpServletResponse response, UserDto userDto) {
        try {
            // 필요한 정보(age, gender)만 Map으로 추출
            Map<String, Integer> activityInfo = new HashMap<>();
            activityInfo.put("age", userDto.getAge());
            activityInfo.put("gender", userDto.getGender());
            
            // Map을 JSON 문자열로 변환
            String activityInfoJson = objectMapper.writeValueAsString(activityInfo);
            
            // JSON 문자열을 Base64로 인코딩 (쿠키 문자 제한 회피)
            String encodedActivityInfo = Base64.getEncoder().encodeToString(activityInfoJson.getBytes(StandardCharsets.UTF_8));
            
            // user_activity_info 쿠키 설정
            ResponseCookie activityCookie = ResponseCookie.from(USER_ACTIVITY_INFO_COOKIE_NAME, encodedActivityInfo)
                    .path("/")
                    .maxAge(DEFAULT_MAX_AGE)
                    .httpOnly(true) // JavaScript에서 접근 가능하도록 설정
                    .secure(true) // HTTPS에서만 전송되도록 설정
                    .sameSite("None")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, activityCookie.toString());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("사용자 활동 정보를 쿠키로 변환하는 중 오류 발생", e);
        }
    }
    
    /**
     * 요청에서 사용자 활동 정보 쿠키 읽기
     */
    @SuppressWarnings("unchecked")
    public static Optional<Map<String, Integer>> getUserActivityInfoFromCookie(HttpServletRequest request) {
        Optional<Cookie> cookieOpt = getCookie(request, USER_ACTIVITY_INFO_COOKIE_NAME);
        
        if (cookieOpt.isPresent()) {
            Cookie cookie = cookieOpt.get();
            try {
                // Base64 디코딩
                byte[] decodedBytes = Base64.getDecoder().decode(cookie.getValue());
                String activityInfoJson = new String(decodedBytes, StandardCharsets.UTF_8);
                
                // JSON을 Map으로 변환
                Map<String, Integer> activityInfo = objectMapper.readValue(activityInfoJson, Map.class);
                return Optional.of(activityInfo);
            } catch (Exception e) {
                // 쿠키 파싱 실패
                return Optional.empty();
            }
        }
        
        return Optional.empty();
    }
}
