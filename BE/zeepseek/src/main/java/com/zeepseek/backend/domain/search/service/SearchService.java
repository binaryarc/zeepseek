package com.zeepseek.backend.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.zeepseek.backend.domain.search.dto.SearchProperty;
import com.zeepseek.backend.domain.search.dto.response.KeywordResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;

    /**
     * 키워드와 페이지네이션 정보를 받아 Elasticsearch에서 검색을 수행합니다.
     * @param keyword 검색어
     * @param page    페이지 번호 (1부터 시작)
     * @param size    페이지 당 결과 수
     * @return 검색 결과 리스트
     */
    public KeywordResponse searchProperties(String keyword, int page, int size) {
        // 페이지 번호가 1부터 시작한다고 가정하면 from 값은 (page - 1) * size
        int from = (page - 1) * size;

        try {
            SearchRequest searchRequest = SearchRequest.of(sr -> sr
                    .index("properties")
                    .from(from)
                    .size(size)
                    .trackTotalHits(t -> t.enabled(true)) // 전체 건수를 추적하도록 설정
                    .query(q -> q
                            .multiMatch(mm -> mm
                                    .query(keyword)
                                    .fields( "dongName", "description", "guName", "roomType")
                                    .type(TextQueryType.BoolPrefix)
                                    .fuzziness("AUTO")
                                    .analyzer("custom_normalizer")
                            )
                    )
            );

            SearchResponse<SearchProperty> searchResponse = elasticsearchClient.search(searchRequest, SearchProperty.class);

            List<SearchProperty> results = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            int totalHits = (int) searchResponse.hits().total().value();

            log.info("검색어 '{}'에 대한 결과 수: {} (페이지: {}, 사이즈: {})", keyword, results.size(), page, size);
            log.info("전체 검색 수: {}",searchResponse.hits().total());

            return KeywordResponse.builder().properties(results).total(totalHits).build();
        } catch (IOException e) {
            log.error("검색 도중 오류 발생", e);
            // 필요에 따라 사용자 정의 예외를 던지거나 빈 객체 반환 가능
            return KeywordResponse.builder().build();
        }
    }


    /**
     * guName과 dongName이 정확하게 일치하는 경우의 데이터만 조회하는 메서드
     * @param guName   검색할 guName 값
     * @param dongName 검색할 dongName 값
     * @param page     페이지 번호 (1부터 시작)
     * @param size     페이지 당 결과 수
     * @return 검색 결과 리스트
     */
    public KeywordResponse searchPropertiesByGuAndDong(String guName, String dongName, int page, int size) {
        int from = (page - 1) * size;

        try {
            SearchRequest searchRequest = SearchRequest.of(sr -> sr
                    .index("properties")
                    .from(from)
                    .size(size)
                    .trackTotalHits(t -> t.enabled(true))
                    .query(q -> q.bool(b -> b
                            .must(List.of(
                                    Query.of(qb -> qb.term(t -> t.field("guName").value(guName))),
                                    Query.of(qb -> qb.match(t -> t.field("dongName").query(dongName)))
                            ))
                    ))
            );

            SearchResponse<SearchProperty> searchResponse = elasticsearchClient.search(searchRequest, SearchProperty.class);
            List<SearchProperty> results = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());
            int totalHits = (int) searchResponse.hits().total().value();

            log.info("guName '{}'와 dongName '{}'에 대한 결과 수: {} (페이지: {}, 사이즈: {})", guName, dongName, results.size(), page, size);
            return KeywordResponse.builder().properties(results).total(totalHits).build();
        } catch (IOException e) {
            log.error("검색 도중 오류 발생", e);
            return KeywordResponse.builder().build();
        }
    }
}
