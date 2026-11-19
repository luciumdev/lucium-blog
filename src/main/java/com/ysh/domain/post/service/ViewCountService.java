package com.ysh.domain.post.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountService {
    static final String BUFFER_KEY = "view_count_buffer";

    private static final String DEDUP_KEY_PREFIX = "visitor:%s:viewed:%d";
    private static final Duration DEDUP_DURATION = Duration.ofHours(1);

    private static final String READ_KEY_REFIX = "visitor:%s:read";
    private static final Duration READ_TTL = Duration.ofDays(90);

    private final StringRedisTemplate redis;

    /**
     * 조회수 증가만 담당. dedup(1시간) 통과 시에만 버퍼 +1.
     *
     * @return dedup 통과 여부 (true = 이번 조회가 실제로 카운트됨)
     */
    public boolean tryIncrement(UUID visitorId, long postId) {
        if (visitorId == null) {
            return false;
        }

        String dedupKey = DEDUP_KEY_PREFIX.formatted(visitorId, postId);
        boolean isFirstView = redis.opsForValue().setIfAbsent(dedupKey, "1", DEDUP_DURATION);
        if (!isFirstView) {
            return false;
        }

        redis.opsForHash().increment(BUFFER_KEY, String.valueOf(postId), 1L);
        return true;
    }

    /**
     * 읽음 기록만 담당. dedup과 무관하게 글을 열면 무조건 기록. 90일 유지.
     * 조회수 카운트(dedup 1시간)와 시간 축이 다르므로 분리.
     */
    public void markRead(UUID visitorId, long postId) {
        if (visitorId == null) {
            return;
        }
        String readKey = READ_KEY_REFIX.formatted(visitorId);
        redis.opsForSet().add(readKey, String.valueOf(postId));
        redis.expire(readKey, READ_TTL);
    }

    /**
     * 아직 flush 안 된, 버퍼에 남아있는 이 글의 증가분.
     * DB 확정값에 더해 실시간 조회수를 응답에 반영하기 위함.
     * flush되면 버퍼가 비므로 이중 계산되지 않음.
     */
    public long getBufferedCount(long postId) {
        Object v = redis.opsForHash().get(BUFFER_KEY, String.valueOf(postId));
        if (v == null) {
            return 0L;
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            log.warn("view_count buffer parse failed. postId={}, value={}", postId, v);
            return 0L;
        }
    }
    /**
     * 현재 버퍼에 쌓인 모든 글의 증가분을 한 번에 조회.
     * 목록 응답에서 N개 글의 실시간 조회수 계산 시 Redis 왕복 1회로 처리.
     *
     * @return postId -> 버퍼 증가분 맵 (버퍼 비어있으면 빈 맵)
     */
    public Map<Long, Long> getAllBufferedCounts() {
        Map<Object, Object> entries = redis.opsForHash().entries(BUFFER_KEY);
        if (entries.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            try {
                long postId = Long.parseLong(e.getKey().toString());
                long delta = Long.parseLong(e.getValue().toString());
                result.put(postId, delta);
            } catch (NumberFormatException ex) {
                log.warn("view_count buffer parse skip. key={}, value={}", e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * Front alreadyRead check
     */
    public boolean hasRead(UUID visitorId, long postId) {
        if (visitorId == null) {
            return false;
        }
        String readKey = READ_KEY_REFIX.formatted(visitorId);
        Boolean isMember = redis.opsForSet().isMember(readKey, String.valueOf(postId));
        return Boolean.TRUE.equals(isMember);
    }
}