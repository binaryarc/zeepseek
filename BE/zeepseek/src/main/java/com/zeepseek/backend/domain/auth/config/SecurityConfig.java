package com.zeepseek.backend.domain.auth.config;

import com.zeepseek.backend.domain.auth.security.jwt.JwtAuthenticationFilter;
import com.zeepseek.backend.domain.auth.security.jwt.JwtTokenProvider;
import com.zeepseek.backend.domain.auth.security.oauth2.CustomOAuth2UserService;
import com.zeepseek.backend.domain.auth.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.zeepseek.backend.domain.auth.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider tokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .authorizeHttpRequests(authorize -> authorize
                        // 로그인 불필요 엔드포인트 - 인증없이 접근 가능

                        // 인증 관련 엔드포인트
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/sessions").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/redirect").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/random-nickname").permitAll()
                        .requestMatchers("/api/v1/zzim/select/**").permitAll()

                        // property 하위 모든 엔드포인트 허용
                        .requestMatchers("/api/v1/property/**").permitAll()

                        // search 하위 모든 엔드포인트 허용
                        .requestMatchers("/api/v1/search/**").permitAll()

                        // viewer 엔드포인트 허용
                        .requestMatchers("/api/v1/viewer/**").permitAll()

                        // dong/{dongId} 엔드포인트만 허용 (구체적인 패턴을 먼저 매칭)
                        .requestMatchers(HttpMethod.GET, "/api/v1/dong/{dongId}").permitAll()

                        // OAuth2 엔드포인트
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()

                        // Swagger UI 및 개발용 엔드포인트
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // 기타 정적 리소스
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/resources/**").permitAll()
                        .requestMatchers("/*.png", "/*.gif", "/*.svg", "/*.jpg", "/*.html", "/*.css", "/*.js").permitAll()

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/oauth2/authorize"))
                        .redirectionEndpoint(endpoint -> endpoint.baseUri("/auth/{provider}/callback"))
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
        ;

        // JWT 필터 추가
        http.addFilterBefore(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://j12e203.p.ssafy.io",
                "https://j12e203.p.ssafy.io"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}