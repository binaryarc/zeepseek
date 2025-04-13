package com.zeepseek.backend.domain.ranking.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class RankingService {

    private final RedisTemplate<String, Object> rankingRedisTemplate;

    public RankingService(@Qualifier("rankingRedisTemplate") RedisTemplate<String, Object> rankingRedisTemplate) {
        this.rankingRedisTemplate = rankingRedisTemplate;
    }

    /**
     * propertyDetail 조회 시 호출되어, 해당 property의 ranking score를 1 증가 시킵니다.
     *
     * @param dongId     해당 property의 동 아이디
     * @param propertyId property의 고유 ID (증가 대상)
     */
    public void incrementPropertyCount(Integer dongId, Integer propertyId) {
        // key 형식 예시: "ranking:101" (동 아이디가 101)
        String key = "ranking:" + dongId;
        // propertyId를 문자열로 변환하여 사용. score를 1 증가시키는 ZINCRBY 명령어를 사용.
        rankingRedisTemplate.opsForZSet().incrementScore(key, propertyId.toString(), 1);
    }

    /**
     * 특정 dongId에 대해 score가 높은 상위 5개의 propertyId를 조회합니다.
     *
     * @param dongId 조회 대상 동 아이디
     * @return propertyId와 해당 score를 포함한 상위 5개 요소
     */
    public Set<ZSetOperations.TypedTuple<Object>> getTop5PropertiesByDongId(String dongId) {
        String key = "ranking:" + dongId;
        // reverseRangeWithScores() 메서드를 사용하면 score 내림차순으로 정렬된 순위를 가져올 수 있음.
        return rankingRedisTemplate.opsForZSet().reverseRangeWithScores(key, 0, 4);
    }
}