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
}
