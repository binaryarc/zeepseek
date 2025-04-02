package com.zeepseek.backend.domain.property.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "property_id", nullable = false)
    private Integer propertyId;

    @Column(name = "transport_count")
    private Integer transportCount;

    @Column(name = "transport_score")
    private Float transportScore;

    @Column(name = "restaurant_count")
    private Integer restaurantCount;

    @Column(name = "restaurant_score")
    private Float restaurantScore;

    @Column(name = "health_count")
    private Integer healthCount;

    @Column(name = "health_score")
    private Float healthScore;

    @Column(name = "convenience_count")
    private Integer convenienceCount;

    @Column(name = "convenience_score")
    private Float convenienceScore;

    @Column(name = "cafe_count")
    private Integer cafeCount;

    @Column(name = "cafe_score")
    private Float cafeScore;

    @Column(name = "chicken_count")
    private Integer chickenCount;

    @Column(name = "chicken_score")
    private Float chickenScore;

    @Column(name = "leisure_count")
    private Integer leisureCount;

    @Column(name = "leisure_score")
    private Float leisureScore;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
