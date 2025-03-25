package com.zeepseek.backend.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferences {
    
    @Id
    private Integer userId;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float safe;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float leisure;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float restaurant;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float health;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float convenience;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float transport;
    
    @Column(columnDefinition = "FLOAT DEFAULT 0.0")
    private Float cafe;
}