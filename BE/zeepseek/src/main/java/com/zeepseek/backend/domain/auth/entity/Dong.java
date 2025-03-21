package com.zeepseek.backend.domain.auth.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "DONG")
public class Dong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dong_id")
    private Long dongId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

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

    @OneToMany(mappedBy = "dong", cascade = CascadeType.ALL)
    private List<Property> properties = new ArrayList<>();
}