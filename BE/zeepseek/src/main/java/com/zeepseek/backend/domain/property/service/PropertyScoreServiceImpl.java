package com.zeepseek.backend.domain.property.service;

import com.zeepseek.backend.domain.property.model.PropertyScore;
import com.zeepseek.backend.domain.property.repository.PropertyScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropertyScoreServiceImpl implements PropertyScoreService{

    private final PropertyScoreRepository propertyScoreRepository;

    @Override
    public PropertyScore findByPropertyId(int propertyId) {
        try {
            return propertyScoreRepository.findByPropertyId(propertyId)
                    .orElseThrow(() -> new ChangeSetPersister.NotFoundException());
        } catch (Exception e) {
            return null;
        }
    }
}
