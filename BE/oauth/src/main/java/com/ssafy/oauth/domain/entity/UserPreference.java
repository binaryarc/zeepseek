package com.ssafy.oauth.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "USER_PREFERENCE")
public class UserPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "safe")
    private Float safe;

    @Column(name = "leisure")
    private Float leisure;

    @Column(name = "restaurant")
    private Float restaurant;

    @Column(name = "health")
    private Float health;

    @Column(name = "convenience")
    private Float convenience;

    @Column(name = "transport")
    private Float transport;

    @Column(name = "cafe")
    private Float cafe;

    @Column(name = "bar")
    private Float bar;
}