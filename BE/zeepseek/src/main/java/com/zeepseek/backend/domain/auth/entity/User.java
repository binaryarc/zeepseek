package com.zeepseek.backend.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "user", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "providerid"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idx;

    @Column(name = "isfirst", columnDefinition = "INT DEFAULT 0")
    private Integer isFirst;

    @Column(name = "isseller", columnDefinition = "INT DEFAULT 0")
    private Integer isSeller;

    @Column(name = "gender", columnDefinition = "INT DEFAULT 0")
    private Integer gender;

    @Column(name = "age", columnDefinition = "INT DEFAULT 0")
    private Integer age;

    @Column(name = "refreshtoken", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "provider", length = 20)
    private String provider;

    @Column(name = "providerid", length = 100)
    private String providerId;
}