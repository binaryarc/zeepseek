package com.zeepseek.backend.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2UserInfoDto {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String providerId;
    private String provider;
    private String nickname;

    public static OAuth2UserInfoDto of(String provider, String nameAttributeKey,
                                       Map<String, Object> attributes) {
        if (provider.equals("kakao")) {
            return ofKakao(nameAttributeKey, attributes);
        } else if (provider.equals("naver")) {
            return ofNaver(nameAttributeKey, attributes);
        }
        throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다.");
    }

    private static OAuth2UserInfoDto ofKakao(String nameAttributeKey, Map<String, Object> attributes) {
        // Kakao는 kakao_account에 사용자 정보가 있고 그 안에 profile이라는 JSON 객체가 있음
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuth2UserInfoDto.builder()
                .attributes(attributes)
                .nameAttributeKey(nameAttributeKey)
                .providerId(attributes.get("id").toString())
                .provider("kakao")
                .nickname((String) kakaoProfile.get("nickname"))
                .build();
    }

    private static OAuth2UserInfoDto ofNaver(String nameAttributeKey, Map<String, Object> attributes) {
        // Naver는 response라는 키 안에 사용자 정보가 담겨있음
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuth2UserInfoDto.builder()
                .attributes(attributes)
                .nameAttributeKey(nameAttributeKey)
                .providerId((String) response.get("id"))
                .provider("naver")
                .nickname((String) response.get("nickname"))
                .build();
    }
}