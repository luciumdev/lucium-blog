package com.ysh.domain.market.client.kis;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.config.redis.RedisLockExecutor;
import com.ysh.domain.market.client.dto.KisTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

/**
 * KIS OAuth2 access token 관리
 * KIS 토큰 정책: 24시간 만료, 동일 앱키로 1분 이내 재발급 -> 에러남 (EGW00133)
 * 분산락 적용
 */
@Slf4j
@Service
public class KisAuthService {

    private static final String TOKEN_CACHE_KEY = "market:kis:access-token";
    private static final String TOKEN_LOCK_KEY = "market:kis:auth-lock";

    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;
    private final RedisLockExecutor lockExecutor;
    private final String appKey;
    private final String appSecret;
    private final Duration cacheTtl;
    private final Duration lockWait;
    private final Duration lockLease;
    private final ObjectMapper objectMapper;

    public KisAuthService(
            @Qualifier("kisRestClient") RestClient restClient,
            StringRedisTemplate redisTemplate,
            RedisLockExecutor lockExecutor,
            @Value("${market.kis.app-key}") String appKey,
            @Value("${market.kis.app-secret}") String appSecret,
            @Value("${market.kis.token.cache-ttl}") Duration cacheTtl,
            @Value("${market.kis.token.lock-wait}") Duration lockWait,
            @Value("${market.kis.token.lock-lease}") Duration lockLease, ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.redisTemplate = redisTemplate;
        this.lockExecutor = lockExecutor;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.cacheTtl = cacheTtl;
        this.lockWait = lockWait;
        this.lockLease = lockLease;
        this.objectMapper = objectMapper;
    }

    /**
     *
     * @return KIS access token (Bearer prefix 없음)
     *
     */
    public String getAccessToken() {
        String cached = readFromCache();
        if (cached != null) {
            return cached;
        }
        log.info("Fetching new access token from KIS");

        return lockExecutor.executeWithLock(
                TOKEN_LOCK_KEY,
                lockWait,
                lockLease,
                () -> {
                    // double-checked: 락 대기 동안 다른 워커가 이미 발급했을 수 있음
                    String afterLock = readFromCache();
                    if (afterLock != null) {
                        log.info("KIS token issued by another worker during lock wait");
                        return afterLock;
                    }
                    return issueAndCache();
                }
        );
    }

    /**
     *
     * @return issueAndCache
     */
    public  String forceRefresh() {
        log.warn("Force refreshing KIS access token");
        String beforeRefresh = readFromCache();
        return lockExecutor.executeWithLock(
                TOKEN_LOCK_KEY,
                lockWait,
                lockLease,
                () -> {
                    String afterLock = readFromCache();
                    if (afterLock != null && !afterLock.equals(beforeRefresh)) {
                        log.info("KIS token already refreshed by another worker — using new cache");
                        return afterLock;
                    }
                    return issueAndCache();
                }
        );
    }

    /**
     * 캐시 재확인
     * @return
     */
    private String readFromCache() {
        try {
            return redisTemplate.opsForValue().get(TOKEN_CACHE_KEY);
        } catch (Exception e) {
            log.error("Failed to read KIS access token from cache: {}", e.getMessage());
            return null;
        }
    }


    private String issueAndCache() {
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(Map.of(
                    "grant_type", "client_credentials",
                    "appkey", appKey,
                    "appsecret", appSecret
            ));
        } catch (JacksonException e) {
            throw new KisAuthException("KIS token body serialization failed: " + e.getMessage(), e);
        }

        KisTokenResponse response;
        try {
            response = restClient.post()
                    .uri("/oauth2/tokenP")
                    .contentType(MediaType.APPLICATION_JSON )
                    .body(jsonBody)
                    .retrieve()
                    .body(KisTokenResponse.class);
        } catch (RestClientException e) {
            throw new KisAuthException("KIS token API call failed: " + e.getMessage(), e);
        }

        if (response == null) {
            throw new KisAuthException("KIS token API returned null body");
        }

        if (response.isError()) {
            log.error("KIS token issue rejected. errorCode={} desc={} appKey.len={}",
                    response.errorCode(),
                    response.errorDescription(),
                    appKey == null ? -1 : appKey.length(),
                    appSecret == null ? -1 : appSecret.length());
            throw new KisAuthException(
                    "KIS token issue rejected: %s - %s".formatted(
                            response.errorCode(), response.errorDescription()));
        }

        if (response.accessToken() == null || response.accessToken().isBlank()) {
            throw new KisAuthException("KIS token API returned no access_token (unexpected response shape)");
        }

        Duration ttl = pickTtl(response.expiresInSeconds());
        try {
            redisTemplate.opsForValue().set(TOKEN_CACHE_KEY, response.accessToken(), ttl);
            log.info("KIS token issued and cached (ttl={})", ttl);
        } catch (Exception e) {
            log.error("KIS token cache save failed (token still returned): {}", e.getMessage());
        }
        return response.accessToken();
    }

    /**
     * cache TTL 결정.
     */
    private Duration pickTtl(Long expiresInSeconds) {
        if (expiresInSeconds == null) {
            return cacheTtl;
        }
        Duration responseDriven = Duration.ofSeconds(expiresInSeconds).minusHours(1);
        if (responseDriven.isNegative() || responseDriven.toMinutes() < 60) {
            return cacheTtl;
        }
        return responseDriven.compareTo(cacheTtl) < 0 ? responseDriven : cacheTtl;
    }

    public static class KisAuthException extends RuntimeException {
        public KisAuthException(String message) {
            super(message);
        }

        public KisAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}

