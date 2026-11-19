package com.ysh.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Redis 기반 분산락 실행기
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockExecutor {

    /**
     * KEYS[1] = lock key, ARGV[1] = expected owner UUID
     *  race condition 방지용
     */
    private static final String UNLOCK_LUA = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else 
                return 0 
            end
            """;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(UNLOCK_LUA, Long.class);
    private static final long POLL_INTERVAL_MS = 1000L;
    private final StringRedisTemplate redisTemplate;

    public <T> T executeWithLock(String lockKey,
                                 Duration waitTime,
                                 Duration leaseTime,
                                 Supplier<T> supplier) {
        String ownerToken = UUID.randomUUID().toString();
        long deadlineMs = System.currentTimeMillis() + waitTime.toMillis();

        while (true) {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, ownerToken, leaseTime);
            if (Boolean.TRUE.equals(acquired)) {
                try {
                    return supplier.get();
                } finally {
                    releaseQuietly(lockKey, ownerToken);
                }
            }
            if (System.currentTimeMillis() >= deadlineMs) {
                throw new LockAcquisitionException("Failed to acquire lock=" + lockKey + " within " + waitTime);
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LockAcquisitionException("Interrupted while waiting for lock=" + lockKey);
            }
        }
    }

    private void releaseQuietly(String lockKey, String ownerToken) {
        try {
            Long released = redisTemplate.execute(
                    UNLOCK_SCRIPT, List.of(lockKey), ownerToken);
            if (released == null || released == 0L) {
                // 두 케이스:
                // 1) leaseTime 초과로 Redis가 이미 자동 만료시킴 (= 작업이 leaseTime보다 오래 걸렸다는 시그널 → leaseTime 늘려야)
                // 2) 다른 워커가 새 락을 가져감 (1과 동일한 원인)
                log.warn("Lock={} not released by owner={} — lease expired during work?", lockKey, ownerToken);
            } else {
                log.debug("Lock released key={} owner={}", lockKey, ownerToken);
            }
        } catch (Exception e) {
            // unlock 실패해도 leaseTime 후 자동 만료
            log.error("Lock release failed key={} cause={}", lockKey, e.getMessage());
        }
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}
