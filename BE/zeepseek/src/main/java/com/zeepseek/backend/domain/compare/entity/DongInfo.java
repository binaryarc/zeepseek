package com.zeepseek.backend.domain.compare.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// MySQL 엔티티 (JPA)
@Entity
@Getter
@Setter
@Table(name = "dong")
public class DongInfo {

    @Id
    @Column(name = "dong_id")
    private Integer dongId;

    private String name;

    @Column(name = "gu_name")
    private String guName;

    private Float safe;
    private Float leisure;
    private Float restaurant;
    private Float health;
    private Float mart;
    private Float convenience;
    private Float transport;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}