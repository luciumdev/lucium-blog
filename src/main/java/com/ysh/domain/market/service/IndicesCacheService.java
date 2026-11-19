package com.ysh.domain.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.domain.market.domain.IndicesData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 4개 지수 시계열 묶음({@link IndicesData}) Redis 캐시.
 *
 * {@link ForexCacheService} 패턴 그대로 — jackson 3.x ObjectMapper로 JSON 직렬화,
 * 캐시 키 {@code market:indices}, TTL은 {@code market.indices.cache-ttl} 설정.
 * Redis만 의존 — 환경 무관 빈 등록.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicesCacheService {

    private static final String INDICES_KEY = "market:indices";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${market.indices.cache-ttl:25h}")
    private Duration ttl;

    public Optional<IndicesData> get() {
        try {
            String json = redisTemplate.opsForValue().get(INDICES_KEY);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, IndicesData.class));
        } catch (Exception e) {
            log.error("Failed to deserialize indices payload: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(IndicesData data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(INDICES_KEY, json, ttl);
        } catch (Exception e) {
            log.error("Failed to serialize indices payload: {}", e.getMessage());
        }
    }

    public void evict() {
        try {
            redisTemplate.delete(INDICES_KEY);
        } catch (Exception e) {
            log.error("Failed to evict indices payload: {}", e.getMessage());
        }
    }
}