package com.ssafy.oauth.security.oauth2;

import com.ssafy.oauth.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class UserPrincipal implements OAuth2User, UserDetails {
    private Long id;
    private String provider;
    private String providerId;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;
    private boolean isFirstLogin;

    // UserDetails 구현 메서드
    @Override
    public String getPassword() {
        return "";  // OAuth2 인증이므로 비밀번호 없음
    }

    @Override
    public String getUsername() {
        return provider + "_" + providerId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // OAuth2User 구현 메서드
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    // User 엔티티로부터 UserPrincipal 생성
    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER")
        );

        // 판매자인 경우 권한 추가
        if (user.getIsSeller() == 1) {
            authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_SELLER")
            );
        }

        return new UserPrincipal(
                user.getIdx(),
                user.getProvider(),
                user.getProviderId(),
                authorities,
                Collections.emptyMap(),
                user.getIsFirst() == 1
        );
    }

    // OAuth2 인증 정보까지 포함하여 UserPrincipal 생성
    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        return new UserPrincipal(
                userPrincipal.getId(),
                userPrincipal.getProvider(),
                userPrincipal.getProviderId(),
                userPrincipal.getAuthorities(),
                attributes,
                user.getIsFirst() == 1
        );
    }
}