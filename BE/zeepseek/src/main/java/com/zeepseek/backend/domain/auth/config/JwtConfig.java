package com.zeepseek.backend.domain.auth.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class JwtConfig {
    @Value("${app.auth.token-secret}")
    private String tokenSecret;

    @Value("${app.auth.access-token-expiration-msec}")
    private long accessTokenExpirationMsec;

    @Value("${app.auth.refresh-token-expiration-msec}")
    private long refreshTokenExpirationMsec;
}