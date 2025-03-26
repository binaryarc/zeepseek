package com.zeepseek.backend.domain.dong.service;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.dong.repository.MongoDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompareService {

    private final MongoDongRepository mongoDongRepository;
    private final GPTSummaryService gptSummaryService;

    public void compareDong(int dong1, int dong2) {
        DongInfoDocs dongInfo1 = mongoDongRepository.findByDongId(dong1);
        DongInfoDocs dongInfo2 = mongoDongRepository.findByDongId(dong2);


    }

}
