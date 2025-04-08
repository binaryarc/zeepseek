package com.zeepseek.backend.domain.auth.security.oauth2;

import com.zeepseek.backend.domain.user.entity.User;
import com.zeepseek.backend.domain.user.repository.UserRepository;
import com.zeepseek.backend.domain.auth.security.UserPrincipal;
import com.zeepseek.backend.domain.auth.security.oauth2.provider.KakaoOAuth2UserInfo;
import com.zeepseek.backend.domain.auth.security.oauth2.provider.NaverOAuth2UserInfo;
import com.zeepseek.backend.domain.auth.security.oauth2.provider.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = oAuth2UserRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuth2UserInfo oAuth2UserInfo;
        if (registrationId.equalsIgnoreCase("kakao")) {
            oAuth2UserInfo = new KakaoOAuth2UserInfo(oAuth2User.getAttributes());
        } else if (registrationId.equalsIgnoreCase("naver")) {
            oAuth2UserInfo = new NaverOAuth2UserInfo(oAuth2User.getAttributes());
        } else {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다.");
        }

        if (!StringUtils.hasText(oAuth2UserInfo.getProviderId())) {
            throw new OAuth2AuthenticationException("Provider ID를 찾을 수 없습니다.");
        }

        Optional<User> userOptional = userRepository.findByProviderAndProviderId(
                oAuth2UserInfo.getProvider(),
                oAuth2UserInfo.getProviderId());

        User user;
        boolean isFirst = false;

        if (userOptional.isPresent()) {
            user = userOptional.get();

            // 사용자가 이미 설문을 완료했는지 확인 (isFirst = 0)
            if (user.getIsFirst() == 1) {
                // 아직 설문을 완료하지 않은 사용자만 소셜 닉네임으로 업데이트
                if (!user.getNickname().equals(oAuth2UserInfo.getNickname())) {
                    log.info("사용자 닉네임 업데이트 (설문 미완료): {} -> {}", user.getNickname(), oAuth2UserInfo.getNickname());
                    user.setNickname(oAuth2UserInfo.getNickname());
                    userRepository.save(user);
                }
            } else {
                // 이미 설문을 완료한 사용자는 닉네임을 업데이트하지 않음
                log.info("사용자가 이미 설문을 완료했으므로 닉네임 유지: {}", user.getNickname());
            }
        } else {
            // 새 사용자 생성
            user = createUser(oAuth2UserInfo);
            isFirst = true;
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user, oAuth2User.getAttributes());
        userPrincipal.setFirst(isFirst);

        return userPrincipal;
    }

    private User createUser(OAuth2UserInfo oAuth2UserInfo) {
        User user = User.builder()
                .provider(oAuth2UserInfo.getProvider())
                .providerId(oAuth2UserInfo.getProviderId())
                .nickname(oAuth2UserInfo.getNickname())
                .isFirst(1) // 처음 가입
                .isSeller(0)
                .gender(0)
                .age(0)
                .build();

        return userRepository.save(user);
    }
}