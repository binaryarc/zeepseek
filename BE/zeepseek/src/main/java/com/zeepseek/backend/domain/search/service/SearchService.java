package com.zeepseek.backend.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.zeepseek.backend.domain.search.dto.SearchProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public List<SearchProperty> searchProperties(String keyword, int page, int size) {
        // 페이지 번호가 1부터 시작한다고 가정하면 from 값은 (page - 1) * size
        int from = (page - 1) * size;

        try {
            SearchRequest searchRequest = SearchRequest.of(sr -> sr
                    .index("properties")
                    .from(from)
                    .size(size)
                    .query(q -> q
                            .multiMatch(mm -> mm
                                    .query(keyword)
                                    .fields( "address", "description", "guName", "roomType")
                                    .type(TextQueryType.BoolPrefix)
                                    .fuzziness("AUTO")
                            )
                    )
            );

            SearchResponse<SearchProperty> searchResponse = elasticsearchClient.search(searchRequest, SearchProperty.class);

            List<SearchProperty> results = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            log.info("검색어 '{}'에 대한 결과 수: {} (페이지: {}, 사이즈: {})", keyword, results.size(), page, size);
            return results;
        } catch (IOException e) {
            log.error("검색 도중 오류 발생", e);
            // 필요에 따라 사용자 정의 예외를 던지거나 빈 리스트 반환 가능
            return Collections.emptyList();
        }
    }
}
