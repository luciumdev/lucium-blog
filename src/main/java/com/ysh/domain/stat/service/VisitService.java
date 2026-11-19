package com.ysh.domain.stat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 방문자 집계 — visitor_id 쿠키 기준 순 방문자(Redis HyperLogLog).
 *
 * <ul>
 *   <li>일별: visit:day:yyyy-MM-dd (KST) — 400일 보관</li>
 *   <li>누적: visit:total — 만료 없음</li>
 * </ul>
 * HLL 은 키당 최대 12KB 고정이고 같은 방문자를 여러 번 넣어도 1명으로 센다(표준 오차 약 0.81%).
 * 기록은 프론트가 페이지 진입 시 POST /v1/stat/visit 로 보낸다 — JS 를 실행하지 않는 봇은 자연히 빠짐.
 * Redis 장애는 집계만 포기하고 요청은 실패시키지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String DAY_KEY = "visit:day:%s";
    private static final String TOTAL_KEY = "visit:total";
    private static final Duration DAY_TTL = Duration.ofDays(400);

    private final StringRedisTemplate redis;

    public void record(UUID visitorId) {
        if (visitorId == null) {
            return;
        }
        String visitor = visitorId.toString();
        String dayKey = dayKey(LocalDate.now(KST));
        try {
            redis.opsForHyperLogLog().add(dayKey, visitor);
            redis.expire(dayKey, DAY_TTL);
            redis.opsForHyperLogLog().add(TOTAL_KEY, visitor);
        } catch (DataAccessException e) {
            log.warn("방문 기록 실패 — 집계만 생략. cause={}", e.getMessage());
        }
    }

    /** 오늘(KST) 순 방문자 */
    public long today() {
        return count(dayKey(LocalDate.now(KST)));
    }

    /** 누적 순 방문자 */
    public long total() {
        return count(TOTAL_KEY);
    }

    private long count(String key) {
        try {
            Long size = redis.opsForHyperLogLog().size(key);
            return size == null ? 0L : size;
        } catch (DataAccessException e) {
            log.warn("방문자 수 조회 실패. key={}, cause={}", key, e.getMessage());
            return 0L;
        }
    }

    private static String dayKey(LocalDate date) {
        return DAY_KEY.formatted(date);
    }
}
