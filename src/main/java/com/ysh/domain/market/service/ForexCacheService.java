package com.ysh.domain.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.domain.market.domain.ForexRates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForexCacheService {

    private static final String FOREX_KEY = "market:forex";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${market.forex.cache-ttl:1h}")
    private Duration ttl;

    public Optional<ForexRates> getForexRates() {
        try {
            String json = redisTemplate.opsForValue().get(FOREX_KEY);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, ForexRates.class));
        } catch (Exception e) {
            log.error("Failed to deserialize forex payload: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(ForexRates rates) {
        try {
            String json = objectMapper.writeValueAsString(rates);
            redisTemplate.opsForValue().set(FOREX_KEY, json, ttl);
        } catch (Exception e) {
            log.error("Failed to serialize forex payload: {}", e.getMessage());
        }
    }

    public void evict() {
        try {
            redisTemplate.delete(FOREX_KEY);
        } catch (Exception e) {
            log.error("Failed to evict forex payload: {}", e.getMessage());
        }
    }
}

