// src/main/java/com/zeepseek/backend/config/WebClientConfig.java
package com.zeepseek.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * WebClient.Builder 빈을 등록하여 애플리케이션 전반에서 WebClient를 쉽게 주입받아 사용할 수 있도록 설정합니다.
     * 필요한 경우 기본 URL, 타임아웃, 로깅 등의 추가 구성을 여기에 포함할 수 있습니다.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
