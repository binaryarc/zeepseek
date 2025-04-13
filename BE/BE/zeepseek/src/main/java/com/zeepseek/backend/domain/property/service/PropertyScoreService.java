package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.model.PropertyScore;

public interface PropertyScoreService {

    PropertyScore findByPropertyId(int propertyId);
}
