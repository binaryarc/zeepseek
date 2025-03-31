package com.zeepseek.backend.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeepseek.backend.domain.auth.dto.TokenDto;
import com.zeepseek.backend.domain.user.dto.UserDto;
import com.zeepseek.backend.domain.user.entity.User;
import com.zeepseek.backend.domain.user.entity.UserRole;
import com.zeepseek.backend.domain.auth.exception.AuthException;
import com.zeepseek.backend.domain.user.repository.UserRepository;
import com.zeepseek.backend.domain.auth.security.UserPrincipal;
import com.zeepseek.backend.domain.auth.security.jwt.JwtTokenProvider;
import com.zeepseek.backend.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService; // UserService 의존성 추가
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String naverRedirectUri;

    /**
     * 리프레시 토큰을 사용하여 새 토큰 발급
     */
    @Transactional
    public TokenDto refreshToken(String refreshToken) {
        // 리프레시 토큰 유효성 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("리프레시 토큰이 유효하지 않습니다.");
        }

        // 방법 1: 리프레시 토큰으로 직접 사용자 조회
        Optional<User> userOptional = userRepository.findByRefreshToken(refreshToken);

        if (userOptional.isEmpty()) {
            // 방법 2: 토큰에서 Claims을 파싱하여 사용자 ID 추출
            try {
                // 토큰에서 Authentication 객체 얻기
                Authentication authentication = tokenProvider.getAuthentication(refreshToken);
                String userId = authentication.getName();
                Integer userIdInt = Integer.parseInt(userId);

                // ID로 사용자 조회
                userOptional = userRepository.findById(userIdInt);

                if (userOptional.isEmpty()) {
                    throw new RuntimeException("존재하지 않는 사용자입니다.");
                }
            } catch (Exception e) {
                throw new RuntimeException("리프레시 토큰 처리 중 오류 발생: " + e.getMessage());
            }
        }

        User user = userOptional.get();

        // DB에 저장된 리프레시 토큰과 일치하는지 확인
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new RuntimeException("저장된 리프레시 토큰과 일치하지 않습니다.");
        }

        // 인증 객체 생성
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 새 토큰 생성
        TokenDto tokenDto = tokenProvider.generateToken(authentication);

        // 새 리프레시 토큰 저장
        user.setRefreshToken(tokenDto.getRefreshToken());
        userRepository.save(user);

        // 유저 정보를 가져와서 TokenDto에 설정
        UserDto userDto = userService.getUserById(user.getIdx());
        tokenDto.setUser(userDto);

        return tokenDto;
    }

    @Override
    @Transactional
    public void logout(String accessToken) {
        log.info("로그아웃 시도: 액세스 토큰 = {}", accessToken.substring(0, 10) + "...");

        // 액세스 토큰 유효성 검사
        if (!tokenProvider.validateToken(accessToken)) {
            log.error("유효하지 않은 액세스 토큰");
            throw new AuthException("Invalid access token");
        }
        log.info("액세스 토큰 유효성 검증 성공");

        Authentication authentication = tokenProvider.getAuthentication(accessToken);
        log.info("인증 객체 추출 성공: {}", authentication);

        String userId = authentication.getName();
        log.info("사용자 ID 추출: {}", userId);

        try {
            Integer userIdInt = Integer.parseInt(userId);
            log.info("사용자 ID 변환 성공: {}", userIdInt);

            // 사용자 조회
            Optional<User> userOpt = userRepository.findById(userIdInt);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("사용자 찾음: idx={}, 현재 리프레시 토큰={}", user.getIdx(),
                        user.getRefreshToken() != null ? user.getRefreshToken().substring(0, 10) + "..." : "null");

                // 리프레시 토큰 설정
                user.setRefreshToken(null);
                log.info("리프레시 토큰 null로 설정");

                // 저장
                User savedUser = userRepository.save(user);
                log.info("사용자 저장 완료, 저장 후 리프레시 토큰: {}", savedUser.getRefreshToken());

                // DB에서 다시 조회하여 확인
                User verifyUser = userRepository.findById(userIdInt).orElse(null);
                log.info("DB에서 재조회한 사용자 리프레시 토큰: {}",
                        verifyUser != null ? verifyUser.getRefreshToken() : "사용자 없음");
            } else {
                log.error("사용자를 찾을 수 없음: ID={}", userIdInt);
            }
        } catch (NumberFormatException e) {
            log.error("사용자 ID 변환 실패: {}", userId, e);
            throw new AuthException("Invalid user ID format");
        }
    }

    @Override
    @Transactional
    public TokenDto processSocialLogin(String authorizationCode, String provider) {
        try {
            if ("kakao".equalsIgnoreCase(provider)) {
                return processKakaoLogin(authorizationCode);
            } else if ("naver".equalsIgnoreCase(provider)) {
                return processNaverLogin(authorizationCode);
            } else {
                throw new AuthException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
            }
        } catch (Exception e) {
            log.error("소셜 로그인 처리 중 오류 발생", e);
            throw new AuthException("소셜 로그인 처리 실패: " + e.getMessage());
        }
    }

    @Override
    public UserDto getCurrentUserByToken(String accessToken) {
        // 액세스 토큰 유효성 검사
        if (!tokenProvider.validateToken(accessToken)) {
            throw new AuthException("Invalid access token");
        }

        // 토큰에서 사용자 ID 추출
        Authentication authentication = tokenProvider.getAuthentication(accessToken);
        String userId = authentication.getName();

        try {
            Integer userIdInt = Integer.parseInt(userId);
            // UserService를 통해 사용자 정보 조회
            return userService.getUserById(userIdInt);
        } catch (NumberFormatException e) {
            throw new AuthException("Invalid user ID format");
        }
    }

    // 이하 비즈니스 로직 - 소셜 로그인 관련 메서드들은 그대로 유지

    private TokenDto processKakaoLogin(String authorizationCode) {
        // 1. 인증 코드로 액세스 토큰 요청
        String accessToken = getKakaoAccessToken(authorizationCode);

        // 2. 액세스 토큰으로 사용자 정보 요청
        Map<String, Object> userAttributes = getKakaoUserInfo(accessToken);

        // 3. 사용자 정보로 DB 사용자 조회 또는 생성
        User user = processKakaoUser(userAttributes);

        // 4. JWT 토큰 생성
        return generateTokenForUser(user);
    }

    private String getKakaoAccessToken(String authorizationCode) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", authorizationCode);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode accessTokenNode = jsonNode.get("access_token");

            if (accessTokenNode == null) {
                log.error("네이버 응답에 access_token이 없습니다. 응답 내용: {}", response.getBody());
                throw new AuthException("네이버 액세스 토큰 획득 실패: 토큰이 응답에 포함되지 않음");
            }

            return accessTokenNode.asText();
        } catch (JsonProcessingException e) {
            log.error("네이버 액세스 토큰 파싱 실패", e);
            throw new AuthException("네이버 액세스 토큰 획득 실패");
        }
    }

    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                String.class
        );

        try {
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (JsonProcessingException e) {
            log.error("카카오 사용자 정보 파싱 실패", e);
            throw new AuthException("카카오 사용자 정보 획득 실패");
        }
    }

    private User processKakaoUser(Map<String, Object> attributes) {
        String providerId = String.valueOf(attributes.get("id"));

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String nickname = (String) profile.get("nickname");

        // DB에서 사용자 조회
        Optional<User> userOptional = userRepository.findByProviderAndProviderId("kakao", providerId);

        User user;
        boolean isFirst = false;

        if (userOptional.isPresent()) {
            // 기존 사용자
            user = userOptional.get();
            // 필요한 경우 사용자 정보 업데이트
            if (!user.getNickname().equals(nickname)) {
                user.setNickname(nickname);
                user = userRepository.save(user);
            }
        } else {
            // 새 사용자 생성
            user = User.builder()
                    .provider("kakao")
                    .providerId(providerId)
                    .nickname(nickname)
                    .isFirst(1)  // 첫 로그인
                    .isSeller(0)
                    .gender(0)
                    .age(0)
                    .build();

            user = userRepository.save(user);
            isFirst = true;
        }

        return user;
    }

    private TokenDto processNaverLogin(String authorizationCode) {
        // 1. 인증 코드로 액세스 토큰 요청
        String accessToken = getNaverAccessToken(authorizationCode);

        // 2. 액세스 토큰으로 사용자 정보 요청
        Map<String, Object> userAttributes = getNaverUserInfo(accessToken);

        // 3. 사용자 정보로 DB 사용자 조회 또는 생성
        User user = processNaverUser(userAttributes);

        // 4. JWT 토큰 생성
        return generateTokenForUser(user);
    }

    private String getNaverAccessToken(String authorizationCode) {
        String tokenUrl = "https://nid.naver.com/oauth2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", naverClientId);
        params.add("client_secret", naverClientSecret);
        params.add("redirect_uri", naverRedirectUri);
        params.add("code", authorizationCode);

        // 파라미터 로깅 추가
        log.info("네이버 토큰 요청 파라미터: {}", params);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        // 응답 로깅 추가
        log.info("네이버 API 응답: {}", response.getBody());

        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            JsonNode accessTokenNode = jsonNode.get("access_token");

            if (accessTokenNode == null) {
                log.error("네이버 응답에 access_token이 없습니다. 응답 내용: {}", response.getBody());
                throw new AuthException("네이버 액세스 토큰 획득 실패: 토큰이 응답에 포함되지 않음");
            }

            return accessTokenNode.asText();
        } catch (JsonProcessingException e) {
            log.error("네이버 액세스 토큰 파싱 실패", e);
            throw new AuthException("네이버 액세스 토큰 획득 실패");
        }
    }

    private Map<String, Object> getNaverUserInfo(String accessToken) {
        String userInfoUrl = "https://openapi.naver.com/v1/nid/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                String.class
        );

        try {
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (JsonProcessingException e) {
            log.error("네이버 사용자 정보 파싱 실패", e);
            throw new AuthException("네이버 사용자 정보 획득 실패");
        }
    }

    private User processNaverUser(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        String providerId = (String) response.get("id");
        String nickname = (String) response.get("nickname");

        // DB에서 사용자 조회
        Optional<User> userOptional = userRepository.findByProviderAndProviderId("naver", providerId);

        User user;
        boolean isFirst = false;

        if (userOptional.isPresent()) {
            // 기존 사용자
            user = userOptional.get();
            // 필요한 경우 사용자 정보 업데이트
            if (!user.getNickname().equals(nickname)) {
                user.setNickname(nickname);
                user = userRepository.save(user);
            }
        } else {
            // 새 사용자 생성
            user = User.builder()
                    .provider("naver")
                    .providerId(providerId)
                    .nickname(nickname)
                    .isFirst(1)  // 첫 로그인
                    .isSeller(0)
                    .gender(0)
                    .age(0)
                    .build();

            user = userRepository.save(user);
            isFirst = true;
        }

        return user;
    }

    private TokenDto generateTokenForUser(User user) {
        // Authentication 객체 생성
        UserPrincipal userPrincipal = UserPrincipal.create(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null,
                Collections.singletonList(new SimpleGrantedAuthority(
                        user.getIsSeller() == 1 ? UserRole.ROLE_SELLER.name() : UserRole.ROLE_USER.name()))
        );

        // JWT 토큰 생성
        TokenDto tokenDto = tokenProvider.generateToken(authentication);

        // 리프레시 토큰 DB에 저장
        user.setRefreshToken(tokenDto.getRefreshToken());
        userRepository.save(user);

        // isFirst 플래그 설정
        tokenDto.setIsFirst(user.getIsFirst());

        //사용자 정보 구성 - UserService 활용
        UserDto userDto = userService.getUserById(user.getIdx());
        tokenDto.setUser(userDto);

        return tokenDto;
    }
}