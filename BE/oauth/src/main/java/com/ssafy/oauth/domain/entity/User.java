package com.ssafy.oauth.domain.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "USER", uniqueConstraints = {
        @UniqueConstraint(name = "unique_social_account", columnNames = {"email", "provider"})
})
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idx")
    private Long idx;

    @Column(name = "isFirst", columnDefinition = "INT DEFAULT 0")
    private Integer isFirst = 0;

    @Column(name = "isSeller", columnDefinition = "INT DEFAULT 0")
    private Integer isSeller = 0;

    @Column(name = "gender", columnDefinition = "INT DEFAULT 0")
    private Integer gender = 0;

    @Column(name = "age", columnDefinition = "INT DEFAULT 0")
    private Integer age = 0;

    @Column(name = "refreshToken", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "provider", length = 20)
    private String provider;

    @Column(name = "providerId", length = 100)
    private String providerId;

    @Builder
    public User(Integer isFirst, Integer isSeller, Integer gender, Integer age,
                String refreshToken, String nickname, String provider,
                String providerId) {
        this.isFirst = isFirst != null ? isFirst : 0;
        this.isSeller = isSeller != null ? isSeller : 0;
        this.gender = gender != null ? gender : 0;
        this.age = age != null ? age : 0;
        this.refreshToken = refreshToken;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
    }

    // 업데이트 메서드
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updateUserInfo(String nickname, Integer gender, Integer age) {
        if (nickname != null) this.nickname = nickname;
        if (gender != null) this.gender = gender;
        if (age != null) this.age = age;
    }

    public void markAsFirstLogin() {
        this.isFirst = 1;
    }

    public void toggleSellerStatus() {
        this.isSeller = this.isSeller == 0 ? 1 : 0;
    }
}