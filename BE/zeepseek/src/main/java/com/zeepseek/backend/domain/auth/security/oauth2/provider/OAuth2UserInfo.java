package com.zeepseek.backend.domain.auth.security.oauth2.provider;

import java.util.Map;

public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getNickname();
    Map<String, Object> getAttributes();
}