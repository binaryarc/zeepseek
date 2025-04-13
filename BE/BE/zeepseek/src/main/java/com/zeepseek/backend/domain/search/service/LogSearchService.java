package com.zeepseek.backend.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.zeepseek.backend.domain.search.document.LogDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LogSearchService {

    private final ElasticsearchClient elasticsearchClient; // 주입

    public LogSearchService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * 지정한 userId와 action("view" 또는 "click") 조건에 맞는 로그 중 가장 최신의 문서를 조회하는 메서드
     *
     * @param userId 조회할 사용자 ID
     * @return 최신 로그 문서 (없으면 null)
     */
    public LogDocument getLatestUserLog(int userId) {
        try {
            // Elasticsearch Query 구성:
            // - 인덱스는 "logs"
            // - userId가 일치하는 문서와 action이 "view" 또는 "click" 인 문서를 대상으로 함
            // - time 필드를 기준으로 내림차순 정렬 후, size를 1로 지정
            SearchRequest searchRequest = SearchRequest.of(sr -> sr
                    .index("logs")
                    .size(1)
                    .sort(s -> s.field(f -> f.field("time").order(SortOrder.Desc)))
                    .query(q -> q.bool(b -> {
                        List<Query> mustQueries = new ArrayList<>();
                        // userId 일치 조건
                        mustQueries.add(Query.of(qb -> qb.term(t -> t.field("userId").value(userId))));
                        // action 필드가 "view" 또는 "click" 인 조건
                        mustQueries.add(Query.of(qb -> qb.terms(t -> t
                                .field("action")
                                .terms(terms -> terms.value(
                                        List.of(FieldValue.of("view"), FieldValue.of("click"))
                                ))
                        )));
                        return b.must(mustQueries);
                    }))
            );

            // Elasticsearch에서 검색 수행
            SearchResponse<LogDocument> searchResponse = elasticsearchClient.search(searchRequest, LogDocument.class);
            List<Hit<LogDocument>> hits = searchResponse.hits().hits();

            if (hits != null && !hits.isEmpty()) {
                // 최신 1건의 로그 문서를 반환
                return hits.get(0).source();
            } else {
                return null;
            }
        } catch (IOException e) {
            // 에러 로그 기록 후 null 반환
            log.error("Error retrieving latest log for userId: {}", userId, e);
            return null;
        }
    }
}
