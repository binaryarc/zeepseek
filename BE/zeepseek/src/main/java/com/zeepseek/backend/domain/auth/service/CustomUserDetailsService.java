package com.zeepseek.backend.domain.auth.service;

import com.zeepseek.backend.domain.user.entity.User;
import com.zeepseek.backend.domain.user.entity.UserRole;
import com.zeepseek.backend.domain.user.repository.UserRepository;
import com.zeepseek.backend.domain.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(Integer.valueOf(userId))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        return UserPrincipal.builder()
                .id(user.getIdx())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority(user.getIsSeller() == 1 ?
                                UserRole.ROLE_SELLER.name() : UserRole.ROLE_USER.name())
                ))
                .build();
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return UserPrincipal.builder()
                .id(user.getIdx())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority(user.getIsSeller() == 1 ?
                                UserRole.ROLE_SELLER.name() : UserRole.ROLE_USER.name())
                ))
                .build();
    }
}