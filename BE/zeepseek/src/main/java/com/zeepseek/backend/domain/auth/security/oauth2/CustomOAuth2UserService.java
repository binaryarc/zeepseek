package com.zeepseek.backend.domain.auth.security.oauth2;

import com.zeepseek.backend.domain.auth.entity.User;
import com.zeepseek.backend.domain.auth.repository.UserRepository;
import com.zeepseek.backend.domain.auth.exception.CustomException;
import com.zeepseek.backend.domain.auth.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception ex) {
            log.error("OAuth2 인증 처리 중 오류 발생: {}", ex.getMessage());
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        // OAuth2 제공자 ID (kakao, naver)
        String provider = oAuth2UserRequest.getClientRegistration().getRegistrationId();

        // 제공자별 속성 추출
        OAuthAttributes attributes = OAuthAttributes.of(provider, oAuth2User.getAttributes());

        if (!StringUtils.hasText(attributes.getProviderId())) {
            throw new CustomException(ErrorCode.SOCIAL_LOGIN_FAILED, "소셜 ID를 찾을 수 없습니다.");
        }

        // 사용자 정보 조회 또는 생성
        User user = getOrCreateUser(attributes, provider);

        // UserPrincipal 객체 생성 및 반환
        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User getOrCreateUser(OAuthAttributes attributes, String provider) {
        // 이미 가입된 회원인지 확인
        Optional<User> userOptional = userRepository.findByProviderAndProviderId(
                provider, attributes.getProviderId());

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();

            // 두 번째 로그인인 경우 isFirst 값을 0으로 업데이트
            if (existingUser.getIsFirst() == 1) {
                existingUser.markAsNotFirstLogin();
                return userRepository.save(existingUser);
            }

            // 기존 사용자 반환
            return existingUser;
        } else {
            // 새 사용자 생성
            User user = User.builder()
                    .provider(provider)
                    .providerId(attributes.getProviderId())
                    .nickname(generateNickname(provider, attributes))
                    .isFirst(1) // 첫 로그인
                    .build();

            return userRepository.save(user);
        }
    }

    // 임시 닉네임 생성 (실제로는 사용자가 추후에 변경할 수 있음)
    private String generateNickname(String provider, OAuthAttributes attributes) {
        return provider + "_user_" + attributes.getProviderId().substring(0, 8);
    }
}