package com.zeepseek.backend.domain.property.controller;

import com.zeepseek.backend.domain.property.model.PropertyScore;
import com.zeepseek.backend.domain.property.service.PropertyScoreServiceImpl;
import com.zeepseek.backend.domain.property.service.PropertyService;
import com.zeepseek.backend.domain.recommend.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/property")
public class PropertyScoreController {
    private final PropertyScoreServiceImpl propertyScoreService;

    public PropertyScoreController(PropertyScoreServiceImpl propertyScoreService) {
        this.propertyScoreService = propertyScoreService;
    }

    @GetMapping("/score/{propertyId}")
    public PropertyScore getPropertyScore(@PathVariable int propertyId){
        return propertyScoreService.findByPropertyId(propertyId);
    }
}
