package com.zeepseek.backend.domain.dong.service;

import com.zeepseek.backend.domain.dong.document.DongCompareDocs;
import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.dong.repository.CompareRepository;
import com.zeepseek.backend.domain.dong.repository.MongoDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompareService {

    private final MongoDongRepository mongoDongRepository;
    private final CompareRepository compareRepository;
    private final GPTSummaryService gptSummaryService;

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

}
