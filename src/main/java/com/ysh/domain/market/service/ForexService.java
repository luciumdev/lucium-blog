package com.ysh.domain.market.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.common.cache.CacheResult;
import com.ysh.domain.market.client.ForexClient;
import com.ysh.domain.market.domain.DataType;
import com.ysh.domain.market.domain.ForexRates;
import com.ysh.domain.market.entity.MarketSnapshot;
import com.ysh.domain.market.repository.MarketSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 환율 수집/저장/조회.
 * 책임 분리 (IndicesService/NewsService와 동일 패턴):
 * {@link #fetchLatest()} — 한국수출입은행 API 호출 (트랜잭션 밖)
 * {@link #persistAndCache(ForexRates)} — DB save + Redis put (트랜잭션 안)
 * orchestration은 호출자(Scheduler/Bootstrap)가 책임
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ForexService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ForexClient forexClient;
    private final MarketSnapshotRepository snapshotRepository;
    private final ForexCacheService cacheService;
    private final ObjectMapper objectMapper;

    /**
     * 어제 forex snapshot - 오늘 00:00 kst 기준
     * @return
     */
    @Transactional(readOnly = true)
    public ForexRates getYesterday() {
        LocalDateTime todayStart = LocalDate.now(KST).atStartOfDay();
        return snapshotRepository
                .findTopByDataTypeAndSnapshotAtBeforeOrderBySnapshotAtDesc(DataType.FOREX, todayStart)
                .map(snapshot -> {
                    try {
                        return objectMapper.readValue(snapshot.getPayload(), ForexRates.class);
                    } catch (JsonProcessingException e) {
                        log.warn("Failed to deserialize yesterday forex. id={}", snapshot.getId(), e);
                        return ForexRates.empty();
                    }
                })
                .orElseGet(() -> {
                    log.info("No yesterday forex snapshot — first run or DB empty. Skip change rate.");
                    return ForexRates.empty();
                });
    }
    /** 한국수출입은행 호출 — 트랜잭션 밖. */
    public ForexRates fetchLatest() {
        return forexClient.fetch().sortedByPriority();
    }

    /** DB save + Redis put — 트랜잭션 안. */
    @Transactional
    public void persistAndCache(ForexRates rates) {
        if (rates.isEmpty()) {
            log.info("Forex rates empty. skip persistence.");
            return;
        }
        try {
            String payloadJson = objectMapper.writeValueAsString(rates);
            snapshotRepository.save(MarketSnapshot.of(DataType.FOREX, payloadJson));
            cacheService.put(rates);
            log.info("Forex snapshot saved. count={}", rates.rates().size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize forex payload", e);
            throw new IllegalStateException("forex payload serialization failed", e);
        }
    }

    public CacheResult<ForexRates> getLatestCacheable() {
        return cacheService.getForexRates()
                .map(CacheResult::hit)
                .orElseGet(() -> CacheResult.miss(loadFromDbAndCache()));
    }

    private ForexRates loadFromDbAndCache() {
        ForexRates fromDb = snapshotRepository.findTopByDataTypeOrderBySnapshotAtDesc(DataType.FOREX)
                .map(snapshot -> {
                    try {
                        return objectMapper.readValue(snapshot.getPayload(), ForexRates.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize forex payload. id={}", snapshot.getId(), e);
                        return ForexRates.empty();
                    }
                })
                .orElse(ForexRates.empty());

        if (!fromDb.isEmpty()) {
            cacheService.put(fromDb);
        }
        return fromDb;
    }
}