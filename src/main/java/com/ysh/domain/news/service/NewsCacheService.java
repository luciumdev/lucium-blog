package com.ysh.domain.news.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCacheService {

    private static final String KEY_PREFIX = "market:news:";
    private static final TypeReference<List<NewsFeed>> NEWS_LIST_TYPE =
            new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${news.cache-ttl:1h}")
    private Duration ttl;

    public Optional<List<NewsFeed>> get(NewsCategory category) {
        try {
            String json = redisTemplate.opsForValue().get(keyFor(category));
            if (json == null) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, NEWS_LIST_TYPE));
        } catch (Exception e) {
            log.warn("News cache read failed. category={}, cause={}", category, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(NewsCategory newsCategory, List<NewsFeed> newsList) {
        try {
            String json = objectMapper.writeValueAsString(newsList);
            redisTemplate.opsForValue().set(keyFor(newsCategory), json, ttl);
        } catch (Exception e) {
            log.error("Failed to serialize news payload: {}", e.getMessage());
        }
    }

    public void evict(NewsCategory category) {
        try {
            redisTemplate.delete(keyFor(category));
        } catch (Exception e) {
            log.warn("News cache evict failed. category={}, cause={}", category, e.getMessage());
        }
    }

    private String keyFor(NewsCategory category) {
        // ECONOMY → "market:news:economy"
        return KEY_PREFIX + category.name().toLowerCase();
    }
}
