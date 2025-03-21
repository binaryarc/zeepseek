package com.zeepseek.backend.domain.auth.dto.response;

import com.zeepseek.backend.domain.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long idx;
    private String nickname;
    private Integer gender;
    private Integer age;
    private Integer isSeller;
    private String provider;

    // from 메서드 명시적 구현
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .idx(user.getIdx())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .age(user.getAge())
                .isSeller(user.getIsSeller())
                .provider(user.getProvider())
                .build();
    }
}