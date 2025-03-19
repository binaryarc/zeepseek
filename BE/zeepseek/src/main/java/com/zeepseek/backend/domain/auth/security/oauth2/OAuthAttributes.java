package com.zeepseek.backend.domain.auth.security.oauth2;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Getter
@Builder
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String providerId;
    private String name;
    private String profileImage;

    /**
     * OAuth2 제공자와 속성에 따라 OAuthAttributes 객체를 생성
     */
    public static OAuthAttributes of(String registrationId, Map<String, Object> attributes) {
        log.info("OAuth Provider: {}", registrationId);

        if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes);
        } else if ("naver".equals(registrationId)) {
            return ofNaver("id", attributes);
        }

        throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
    }

    /**
     * 카카오 로그인 정보 파싱
     */
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        log.info("Kakao OAuth attributes: {}", attributes);

        // ID 추출
        Long id = (Long) attributes.get("id");
        String providerId = id != null ? id.toString() : null;

        // 프로필 정보 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> kakaoProfile = null;

        if (kakaoAccount != null) {
            kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");
        }

        String nickname = null;
        String profileImageUrl = null;

        if (kakaoProfile != null) {
            nickname = (String) kakaoProfile.get("nickname");
            profileImageUrl = (String) kakaoProfile.get("profile_image_url");
        }

        return OAuthAttributes.builder()
                .providerId(providerId)
                .name(nickname)
                .profileImage(profileImageUrl)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    /**
     * 네이버 로그인 정보 파싱
     */
    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        log.info("Naver OAuth attributes: {}", attributes);

        // 네이버는 response 안에 사용자 정보가 있음
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        if (response == null) {
            throw new IllegalArgumentException("네이버 OAuth 응답에 사용자 정보가 없습니다.");
        }

        String providerId = (String) response.get("id");
        String nickname = (String) response.get("nickname");
        String profileImageUrl = (String) response.get("profile_image");

        return OAuthAttributes.builder()
                .providerId(providerId)
                .name(nickname)
                .profileImage(profileImageUrl)
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }
}