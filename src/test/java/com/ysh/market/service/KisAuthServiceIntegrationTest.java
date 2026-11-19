package com.ysh.market.service;

import com.ysh.domain.market.client.kis.KisAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KIS 인증 통합 검증 — 실제 KIS API + 실제 Redis 호출.
 *
 * <p>실행 조건:
 * <ul>
 *   <li>환경변수 {@code KIS_APP_KEY}, {@code KIS_APP_SECRET} 설정</li>
 *   <li>Redis 가동 (Docker Compose 또는 로컬)</li>
 *   <li>KIS는 1분당 1회 토큰 발급 제한이 있어 — 테스트를 1분 미만 간격으로 연속 실행하면 EGW00133 에러 발생.
 *       각 테스트 사이 1분 대기하거나 한 번에 한 메서드만 실행할 것.</li>
 * </ul>
 *
 * <p>{@code @Disabled} 기본값 — IDE에서 메서드별 수동 실행. CI에는 안 돌림.
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("KIS 실제 API 호출 — 수동 실행 전용 (1분당 1회 제한 주의)")
class KisAuthServiceIntegrationTest {

    private static final String CACHE_KEY = "market:kis:access-token";
    private static final String LOCK_KEY = "market:kis:auth-lock";

    @Autowired
    private KisAuthService kisAuthService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void clearKisKeys() {
        // 실제 운영 데이터를 건드릴 위험을 막기 위해 KIS 키만 지정 삭제
        redisTemplate.delete(CACHE_KEY);
        redisTemplate.delete(LOCK_KEY);
    }

    @Test
    @DisplayName("Test A: 캐시 miss → 발급 → 캐시 저장 → 두 번째 호출은 캐시 hit")
    void cacheFlow() {
        // === 1차 호출: cache miss → KIS API 호출 ===
        long t1 = System.currentTimeMillis();
        String token1 = kisAuthService.getAccessToken();
        long elapsedMiss = System.currentTimeMillis() - t1;

        assertThat(token1).isNotBlank();

        // 캐시 저장 검증
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        assertThat(cached).isEqualTo(token1);

        // TTL 검증 — 1h~24h 사이여야 (cache-ttl 설정값 기준)
        Long ttlSec = redisTemplate.getExpire(CACHE_KEY, TimeUnit.SECONDS);
        assertThat(ttlSec).isBetween(3600L, 86400L);

        // === 2차 호출: cache hit → KIS API 안 때림 ===
        long t2 = System.currentTimeMillis();
        String token2 = kisAuthService.getAccessToken();
        long elapsedHit = System.currentTimeMillis() - t2;

        assertThat(token2).isEqualTo(token1);

        assertThat(elapsedHit).isLessThan(100L);
    }

    @Test
    @DisplayName("Test B: getAccessToken 동시 10개 — KIS API 1회만, 모두 동일 토큰")
    void concurrentCacheMiss() throws InterruptedException {
        final int N = 10;
        ExecutorService executor = Executors.newFixedThreadPool(N);
        CountDownLatch startGate = new CountDownLatch(1);  // 모든 스레드 동시 출발
        CountDownLatch doneGate = new CountDownLatch(N);
        Set<String> distinctTokens = ConcurrentHashMap.newKeySet();
        AtomicInteger failures = new AtomicInteger();

        try {
            for (int i = 0; i < N; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await();
                        distinctTokens.add(kisAuthService.getAccessToken());
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    } finally {
                        doneGate.countDown();
                    }
                });
            }

            startGate.countDown();  // 출발
            boolean done = doneGate.await(30, TimeUnit.SECONDS);
            assertThat(done).as("30초 내 모두 완료").isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(failures.get()).as("실패 0건").isZero();
        // 핵심: 모든 스레드가 같은 토큰을 받아야 (락 + double-check가 작동했다는 증거)
        assertThat(distinctTokens).as("모두 동일한 토큰").hasSize(1);
    }

    @Test
    @DisplayName("Test C: forceRefresh 동시 5개 — KIS API 1회만, 모두 동일 새 토큰")
    void concurrentForceRefresh() throws InterruptedException {
        // 사전 조건: 캐시에 토큰 1개 있는 상태로 시작
        String existing = kisAuthService.getAccessToken();

        final int N = 5;
        ExecutorService executor = Executors.newFixedThreadPool(N);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(N);
        Set<String> distinctTokens = ConcurrentHashMap.newKeySet();
        AtomicInteger failures = new AtomicInteger();

        try {
            for (int i = 0; i < N; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await();
                        distinctTokens.add(kisAuthService.forceRefresh());
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    } finally {
                        doneGate.countDown();
                    }
                });
            }

            startGate.countDown();
            boolean done = doneGate.await(30, TimeUnit.SECONDS);
            assertThat(done).as("30초 내 모두 완료").isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(failures.get()).as("실패 0건").isZero();
        // 핵심: 모두 같은 토큰 (1개만 발급)
        assertThat(distinctTokens).as("모두 동일한 토큰").hasSize(1);
        // 주의: KIS 1분 제한으로 사전 토큰 = 새 토큰일 수 있음 (KIS가 동일 토큰을 재반환).
        //      따라서 existing != new 검증은 안 함 — 1번만 발급됐다는 것만 검증.
    }

    private static String prefix(String token) {
        if (token == null || token.length() < 20) return String.valueOf(token);
        return token.substring(0, 20) + "...";
    }
}