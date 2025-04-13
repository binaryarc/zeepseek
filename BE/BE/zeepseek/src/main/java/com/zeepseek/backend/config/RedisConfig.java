package com.zeepseek.backend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Property 용 redis
     */
    @Bean
    @Primary
    public RedisConnectionFactory propertyredisConnectionFactory() {
        // 예: 호스트명은 property_redis, 포트는 6379
        return new LettuceConnectionFactory("property_redis", 6379);
    }

    @Bean
    @Primary
    public CacheManager propertyCacheManager(
            @Qualifier("propertyredisConnectionFactory") RedisConnectionFactory connectionFactory) {
        // 캐시 엔트리 TTL을 20분으로 설정하고 JSON 직렬화를 사용합니다.
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(20))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                );
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Ranking 용 Redis 연결 추가 (ranking_redis)
     */
    @Bean
    public RedisConnectionFactory rankingRedisConnectionFactory() {
        // Docker Compose에 정의된 ranking_redis 서비스를 사용합니다.
        // 내부 포트는 6378로 지정되어 있습니다.
        return new LettuceConnectionFactory("ranking_redis", 6378);
    }

    /**
     * Ranking Redis를 위한 CacheManager 구성
     */
    @Bean
    public CacheManager rankingCacheManager(
            @Qualifier("rankingRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        // ranking 캐시 엔트리 TTL을 60분으로 설정하고, JSON 직렬화 사용
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                );
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
