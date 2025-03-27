package com.zeepseek.backend.domain.auth.security;

import com.zeepseek.backend.domain.auth.entity.User;
import com.zeepseek.backend.domain.auth.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements OAuth2User, UserDetails {
    private Integer id;
    private String nickname;
    private String provider;
    private String providerId;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    @Setter
    private boolean isFirst = false;

    public UserPrincipal(Integer id, String nickname, String provider, String providerId,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getIsSeller() == 1 ? UserRole.ROLE_SELLER.name() : UserRole.ROLE_USER.name()));

        return new UserPrincipal(
                user.getIdx(),
                user.getNickname(),
                user.getProvider(),
                user.getProviderId(),
                authorities
        );
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        userPrincipal.setAttributes(attributes);
        return userPrincipal;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getName() {
        return id.toString();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return id.toString();
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
}