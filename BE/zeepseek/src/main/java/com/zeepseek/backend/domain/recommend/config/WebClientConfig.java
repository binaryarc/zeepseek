package com.zeepseek.backend.domain.recommend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${recommendation.api.host:http://recommend_container}")
    private String recommendationApiHost;

    @Value("${recommendation.api.port:8000}")
    private int recommendationApiPort;

    @Bean
    public WebClient recommendationWebClient(WebClient.Builder webClientBuilder) {
        String baseUrl = String.format("%s:%d/recommend", recommendationApiHost, recommendationApiPort);
        
        return webClientBuilder
            .baseUrl(baseUrl)
            .build();
    }
}