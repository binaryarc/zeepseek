package com.zeepseek.backend.domain.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.zeepseek.backend.domain.search.dto.SearchProperty;
import com.zeepseek.backend.domain.search.dto.response.KeywordResponse;
import com.zeepseek.backend.domain.zzim.document.DongZzimDoc;
import com.zeepseek.backend.domain.zzim.document.PropertyZzimDoc;
import com.zeepseek.backend.domain.zzim.service.ZzimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final ZzimService zzimService;
    /**
     * 키워드와 페이지네이션 정보를 받아 Elasticsearch에서 검색을 수행합니다.
     * @param keyword 검색어
     * @param page    페이지 번호 (1부터 시작)
     * @param size    페이지 당 결과 수
     * @return 검색 결과 리스트
     */
    public KeywordResponse searchProperties(String keyword, int page, int size, String roomTypeFilter, int userId) {
        // 페이지 번호가 1부터 시작한다고 가정하면 from 값은 (page - 1) * size
        int from = (page - 1) * size;

        try {
            SearchRequest searchRequest = SearchRequest.of(sr -> sr
                    .index("properties")
                    .from(from)
                    .size(size)
                    .trackTotalHits(t -> t.enabled(true)) // 전체 건수를 추적하도록 설정
                    .query(q -> q.bool(b -> {
                        // 키워드 검색: dongName, description, guName, roomType 필드에서 검색
                        b.must(mu -> mu.multiMatch(mm -> mm
                                .query(keyword)
                                .fields("dongName", "description", "guName", "roomType")
                                .type(TextQueryType.BoolPrefix)
                                .fuzziness("AUTO")
                                .analyzer("custom_normalizer")
                        ));

                        // roomType 필터 조건 추가
                        if (roomTypeFilter != null && !roomTypeFilter.isEmpty()) {
                            if ("원룸/투룸".equals(roomTypeFilter)) {
                                // computedRoomType 필드가 "원룸" 또는 "투룸"인 경우를 OR 조건으로 추가
                                b.filter(f -> f.bool(bf -> bf
                                        .should(s -> s.term(t -> t.field("computedRoomType.keyword").value("원룸")))
                                        .should(s -> s.term(t -> t.field("computedRoomType.keyword").value("투룸")))
                                        .minimumShouldMatch("1")
                                ));
                            } else if ("주택/빌라".equals(roomTypeFilter)) {
                                // roomType 필드가 "빌라", "상가주택", "단독/다가구" 중 하나인 경우를 OR 조건으로 추가
                                b.filter(f -> f.bool(bf -> bf
                                        .should(s -> s.term(t -> t.field("roomType.keyword").value("빌라")))
                                        .should(s -> s.term(t -> t.field("roomType.keyword").value("상가주택")))
                                        .should(s -> s.term(t -> t.field("roomType.keyword").value("단독/다가구")))
                                        .minimumShouldMatch("1")
                                ));
                            } else {
                                // 그 외의 경우는 roomType 필드에서 단일 값 필터 적용
                                b.filter(f -> f.term(t -> t.field("roomType.keyword").value(roomTypeFilter)));
                            }
                        }
                        return b;
                    }))
            );

            // 1. Elasticsearch 검색 결과 가져오기
            SearchResponse<SearchProperty> searchResponse = elasticsearchClient.search(searchRequest, SearchProperty.class);
            List<SearchProperty> results = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            // 2. 사용자 찜 정보 불러오기
            List<PropertyZzimDoc> propertyZzimDocs = zzimService.userSelectPropertyList(userId);

            // 3. 찜한 매물의 ID를 Set으로 변환 (PropertyZzimDoc에는 propertyId 필드가 존재)
            Set<Integer> likedPropertyIds = propertyZzimDocs.stream()
                    .map(PropertyZzimDoc::getPropertyId)
                    .collect(Collectors.toSet());

            // 4. 각 검색 결과에 대해 사용자가 찜한 매물인지 isLiked 필드 설정
            results.forEach(property -> {
                // SearchProperty 클래스의 propertyId 필드 사용
                boolean isLiked = likedPropertyIds.contains(property.getPropertyId());
                property.setLiked(isLiked);
            });

            int totalHits = (int) searchResponse.hits().total().value();

            log.info("검색어 '{}'에 대한 결과 수: {} (페이지: {}, 사이즈: {})", keyword, results.size(), page, size);
            log.info("전체 검색 수: {}", searchResponse.hits().total());

            return KeywordResponse.builder().properties(results).total(totalHits).build();
        } catch (IOException e) {
            log.error("검색 도중 오류 발생", e);
            return KeywordResponse.builder().build();
        }
    }



    /**
     * guName과 dongName이 정확하게 일치하는 경우의 데이터만 조회하는 메서드
     * @param guName           검색할 guName 값
     * @param dongName         검색할 dongName 값
     * @param page             페이지 번호 (1부터 시작)
     * @param size             페이지 당 결과 수
     * @param roomTypeFilter   roomType 필터 조건 (예: "원룸/투룸", "빌라/주택" 등)
     * @return 검색 결과 리스트
     */
    public KeywordResponse searchPropertiesByGuAndDong(String guName, String dongName, int page, int size, String roomTypeFilter, int userId) {
        int from = (page - 1) * size;

        try {
            SearchRequest searchRequest = SearchRequest.of(sr -> sr
                    .index("properties")
                    .from(from)
                    .size(size)
                    .trackTotalHits(t -> t.enabled(true))
                    .query(q -> q.bool(b -> {

                        // must 절에 추가할 조건들을 리스트에 담음
                        List<Query> mustQueries = new ArrayList<>();
                        mustQueries.add(Query.of(qb -> qb.term(t -> t.field("guName").value(guName))));

                        // dongName이 null이거나 빈 문자열이 아니면 조건 추가
                        if (dongName != null && !dongName.isEmpty()) {
                            mustQueries.add(Query.of(qb -> qb.match(t -> t.field("dongName").query(dongName))));
                        }

                        b.must(mustQueries);

                        // roomType 필터 조건 추가
                        if (roomTypeFilter != null && !roomTypeFilter.isEmpty()) {
                            if ("원룸/투룸".equals(roomTypeFilter)) {
                                b.filter(f -> f.bool(bf -> bf
                                        .should(s -> s.term(t -> t.field("computedRoomType.keyword").value("원룸")))
                                        .should(s -> s.term(t -> t.field("computedRoomType.keyword").value("투룸")))
                                        .minimumShouldMatch("1")
                                ));
                            } else if ("주택/빌라".equals(roomTypeFilter)) {
                                b.filter(f -> f.bool(bf -> bf
                                        .should(s -> s.term(t -> t.field("roomType.keyword").value("빌라")))
                                        .should(s -> s.term(t -> t.field("roomType.keyword").value("상가주택")))
                                        .should(s -> s.term(t -> t.field("roomType.keyword").value("단독/다가구")))
                                        .minimumShouldMatch("1")
                                ));
                            } else {
                                b.filter(f -> f.term(t -> t.field("roomType.keyword").value(roomTypeFilter)));
                            }
                        }
                        return b;
                    }))
            );

            // 1. Elasticsearch 검색 결과 가져오기
            SearchResponse<SearchProperty> searchResponse = elasticsearchClient.search(searchRequest, SearchProperty.class);
            List<SearchProperty> results = searchResponse.hits().hits().stream()
                    .map(Hit::source)
                    .collect(Collectors.toList());

            // 2. 사용자 찜 정보 불러오기
            List<PropertyZzimDoc> propertyZzimDocs = zzimService.userSelectPropertyList(userId);

            // 3. 찜한 매물의 ID를 Set으로 변환 (PropertyZzimDoc에는 propertyId 필드가 존재)
            Set<Integer> likedPropertyIds = propertyZzimDocs.stream()
                    .map(PropertyZzimDoc::getPropertyId)
                    .collect(Collectors.toSet());

            // 4. 각 검색 결과에 대해 사용자가 찜한 매물인지 isLiked 필드 설정
            results.forEach(property -> {
                // SearchProperty 클래스의 propertyId 필드 사용
                boolean isLiked = likedPropertyIds.contains(property.getPropertyId());
                property.setLiked(isLiked);
            });

            int totalHits = (int) searchResponse.hits().total().value();

            log.info("guName '{}'와 dongName '{}'에 대한 결과 수: {} (페이지: {}, 사이즈: {})", guName, dongName, results.size(), page, size);
            return KeywordResponse.builder().properties(results).total(totalHits).build();
        } catch (IOException e) {
            log.error("검색 도중 오류 발생", e);
            return KeywordResponse.builder().build();
        }
    }
}
