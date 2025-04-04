package com.zeepseek.backend.domain.dong.service;

import com.zeepseek.backend.domain.dong.document.DongCompareDocs;
import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.dong.document.PropertyCompareDocs;
import com.zeepseek.backend.domain.dong.repository.CompareRepository;
import com.zeepseek.backend.domain.dong.repository.MongoDongRepository;
import com.zeepseek.backend.domain.dong.repository.PropertyCompareRepository;
import com.zeepseek.backend.domain.property.model.Property;
import com.zeepseek.backend.domain.property.model.PropertyScore;
import com.zeepseek.backend.domain.property.service.PropertyScoreServiceImpl;
import com.zeepseek.backend.domain.property.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompareService {

    private final MongoDongRepository mongoDongRepository;
    private final CompareRepository compareRepository;
    private final PropertyCompareRepository propertyCompareRepository;
    private final GPTSummaryService gptSummaryService;
    private final PropertyScoreServiceImpl propertyScoreService;

    public DongCompareDocs compareDong(int dong1, int dong2) {
        DongInfoDocs dongInfo1 = mongoDongRepository.findByDongId(dong1);
        DongInfoDocs dongInfo2 = mongoDongRepository.findByDongId(dong2);

        DongCompareDocs dongCompareDocs = compareRepository.findByCompareId(dong1 + dong2);

        if(dongCompareDocs == null) {
            String compareSummary = gptSummaryService.getSummaryForDongCompare(dongInfo1, dongInfo2);

            DongCompareDocs dongCompareInfo = new DongCompareDocs();
            dongCompareInfo.setCompareId(dong1 + dong2);
            dongCompareInfo.setCompareSummary(compareSummary);

            compareRepository.save(dongCompareInfo);
            return dongCompareInfo;
        } else {
            return dongCompareDocs;
        }

    }

    public PropertyCompareDocs compareProperty(int prop1, int prop2) {
        PropertyScore score1 = propertyScoreService.findByPropertyId(prop1);
        PropertyScore score2 = propertyScoreService.findByPropertyId(prop2);

        PropertyCompareDocs propertyCompareDocs = propertyCompareRepository.findByCompareId(prop1 + prop2);

        if(propertyCompareDocs == null) {
            String compareSummary = gptSummaryService.getSummaryForPropertyCompare(score1, score2);

            PropertyCompareDocs propertyCompareInfo = new PropertyCompareDocs();
            propertyCompareInfo.setCompareId(prop1 + prop2);
            propertyCompareInfo.setCompareSummary(compareSummary);

            propertyCompareRepository.save(propertyCompareInfo);
            return propertyCompareInfo;
        } else {
            return propertyCompareDocs;
        }
    }

}
