package com.ysh.domain.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.domain.market.domain.Commodities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 원자재 Redis 캐시 전담
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommoditiesCacheService {
    private static final String COMMODITIES_KEY = "market:commodities";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${market.commodities.cache-ttl:25h}")
    private Duration ttl;

    public Optional<Commodities> getCommodities() {
        try {
            String json = redisTemplate.opsForValue().get(COMMODITIES_KEY);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, Commodities.class));
        } catch (Exception e) {
            log.error("Failed to deserialize commodities payload: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(Commodities commodities) {
        try {
            String json = objectMapper.writeValueAsString(commodities);
            redisTemplate.opsForValue().set(COMMODITIES_KEY, json, ttl);
        } catch (Exception e) {
            log.error("Failed to serialize commodities payload: {}", e.getMessage());
        }
    }

    public void evict() {
        try {
            redisTemplate.delete(COMMODITIES_KEY);
        } catch (Exception e) {
            log.error("Failed to evict commodities payload: {}", e.getMessage());
        }
    }
}
