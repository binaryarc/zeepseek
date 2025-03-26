package com.zeepseek.backend.domain.dong.service;

import com.zeepseek.backend.domain.dong.document.DongInfoDocs;
import com.zeepseek.backend.domain.dong.entity.DongInfo;
import com.zeepseek.backend.domain.dong.repository.MongoDongRepository;
import com.zeepseek.backend.domain.dong.repository.MySQLDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DongMigrationService {

    private final MySQLDongRepository mysqlDongRepository;
    private final MongoDongRepository mongoDongRepository;
    private final GPTSummaryService gptSummaryService;

    /**
     * 매월 1일 자정에 MySQL의 동네 정보를 MongoDB로 마이그레이션하며 GPT 요약 정보를 업데이트합니다.
     */
    public void migrateAndUpdateDongData() {
        List<DongInfo> mysqlDongList = mysqlDongRepository.findAll();

        List<DongInfoDocs> mongoDongDocs = mysqlDongList.stream().map(dong -> {
            DongInfoDocs doc = new DongInfoDocs();
            doc.setDongId(dong.getDongId());
            doc.setName(dong.getName());
            doc.setGuName(dong.getGuName());
            doc.setSafe(dong.getSafe());
            doc.setLeisure(dong.getLeisure());
            doc.setRestaurant(dong.getRestaurant());
            doc.setHealth(dong.getHealth());
            doc.setMart(dong.getMart());
            doc.setConvenience(dong.getConvenience());
            doc.setTransport(dong.getTransport());
            doc.setUpdatedAt(dong.getUpdatedAt());
            // GPT API 호출하여 요약 정보 설정
            String summary = gptSummaryService.getSummaryForDong(dong);
            doc.setSummary(summary);
            return doc;
        }).collect(Collectors.toList());

        mongoDongRepository.saveAll(mongoDongDocs);
    }
}