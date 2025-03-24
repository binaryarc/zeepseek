package com.zeepseek.backend.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeepseek.backend.domain.auth.dto.TokenDto;
import com.zeepseek.backend.domain.auth.dto.UserDto;
import com.zeepseek.backend.domain.auth.entity.User;
import com.zeepseek.backend.domain.auth.entity.UserRole;
import com.zeepseek.backend.domain.auth.exception.AuthException;
import com.zeepseek.backend.domain.auth.repository.UserRepository;
import com.zeepseek.backend.domain.auth.security.UserPrincipal;
import com.zeepseek.backend.domain.auth.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    @Override
    @Transactional
    public TokenDto refreshToken(String refreshToken) {
        // 리프레시 토큰 유효성 검사
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new AuthException("Invalid refresh token");
        }

        // 리프레시 토큰으로 사용자 조회
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new AuthException("User not found with this refresh token"));

        // Authentication 객체 생성
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal, null,
                Collections.singletonList(new SimpleGrantedAuthority(
                        user.getIsSeller() == 1 ? UserRole.ROLE_SELLER.name() : UserRole.ROLE_USER.name()))
        );

        // 새 토큰 발급
        TokenDto tokenDto = tokenProvider.generateToken(authentication);

        // 리프레시 토큰 DB 업데이트
        user.setRefreshToken(tokenDto.getRefreshToken());
        userRepository.save(user);

        // isFirst 설정
        tokenDto.setIsFirst(user.getIsFirst());

        return tokenDto;
    }

    @Override
    @Transactional
    public void logout(String accessToken) {
        // 액세스 토큰 유효성 검사
        if (!tokenProvider.validateToken(accessToken)) {
            throw new AuthException("Invalid access token");
        }

        Authentication authentication = tokenProvider.getAuthentication(accessToken);
        String userId = authentication.getName();

        // 사용자의 리프레시 토큰 삭제
        userRepository.findById(Integer.parseInt(userId))
                .ifPresent(user -> {
                    user.setRefreshToken(null);
                    userRepository.save(user);
                });
    }

    @Override
    @Transactional
    public UserDto updateUser(Integer userId, UserDto userDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        // 업데이트 가능한 필드만 변경
        if (userDto.getNickname() != null) {
            user.setNickname(userDto.getNickname());
        }
        if (userDto.getGender() != null) {
            user.setGender(userDto.getGender());
        }
        if (userDto.getAge() != null) {
            user.setAge(userDto.getAge());
        }
        if (userDto.getIsSeller() != null) {
            user.setIsSeller(userDto.getIsSeller());
        }

        User updatedUser = userRepository.save(user);

        return UserDto.builder()
                .idx(updatedUser.getIdx())
                .nickname(updatedUser.getNickname())
                .gender(updatedUser.getGender())
                .age(updatedUser.getAge())
                .isFirst(updatedUser.getIsFirst())
                .isSeller(updatedUser.getIsSeller())
                .provider(updatedUser.getProvider())
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(Integer userId) {
        userRepository.findById(userId)
                .ifPresent(userRepository::delete);
    }

    @Override
    @Transactional
    public UserDto processFirstLoginData(Integer userId, UserDto userDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        // 첫 로그인 데이터 처리
        if (userDto.getNickname() != null) {
            user.setNickname(userDto.getNickname());
        }
        if (userDto.getGender() != null) {
            user.setGender(userDto.getGender());
        }
        if (userDto.getAge() != null) {
            user.setAge(userDto.getAge());
        }

        // 첫 로그인 플래그 변경
        user.setIsFirst(0);

        User updatedUser = userRepository.save(user);

        return UserDto.builder()
                .idx(updatedUser.getIdx())
                .nickname(updatedUser.getNickname())
                .gender(updatedUser.getGender())
                .age(updatedUser.getAge())
                .isFirst(updatedUser.getIsFirst())
                .isSeller(updatedUser.getIsSeller())
                .provider(updatedUser.getProvider())
                .build();
    }

    @Override
    public UserDto getCurrentUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        return UserDto.builder()
                .idx(user.getIdx())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .age(user.getAge())
                .isFirst(user.getIsFirst())
                .isSeller(user.getIsSeller())
                .provider(user.getProvider())
                .build();
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
            return jsonNode.get("access_token").asText();
        } catch (JsonProcessingException e) {
            log.error("카카오 액세스 토큰 파싱 실패", e);
            throw new AuthException("카카오 액세스 토큰 획득 실패");
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

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        try {
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("access_token").asText();
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

        return tokenDto;
    }
}