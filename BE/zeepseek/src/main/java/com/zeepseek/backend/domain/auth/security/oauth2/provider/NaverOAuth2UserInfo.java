package com.zeepseek.backend.domain.auth.security.oauth2.provider;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {
    private Map<String, Object> attributes;

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return (String) response.get("id");
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getNickname() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return (String) response.get("nickname");
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}