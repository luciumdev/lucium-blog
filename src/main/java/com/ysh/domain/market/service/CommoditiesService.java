package com.ysh.domain.market.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.common.cache.CacheResult;
import com.ysh.domain.market.client.CommoditiesClient;
import com.ysh.domain.market.domain.Commodities;
import com.ysh.domain.market.domain.DataType;
import com.ysh.domain.market.entity.MarketSnapshot;
import com.ysh.domain.market.repository.MarketSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 원자재 데이터 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommoditiesService {
    private final CommoditiesClient commoditiesClient;
    private final MarketSnapshotRepository snapshotRepository;
    private final CommoditiesCacheService cacheService;
    private final ObjectMapper objectMapper;

    /**
     * Cache-aside 패턴 — Redis hit 우선, miss 시 DB 최근 스냅샷 fallback → cache 채움.
     * Redis 재시작/만료 직후에도 마지막 수집 데이터 노출. ForexService 패턴 일관.
     */
    public CacheResult<Commodities> getLatestCacheable() {
        return cacheService.getCommodities()
                .map(CacheResult::hit)
                .orElseGet(() -> CacheResult.miss(loadFromDbAndCache()));
    }

    /** 외부 API 호출 — 트랜잭션 밖. CommoditiesClient 내부에서 부분 실패 처리. */
    public Commodities fetchLatest() {
        return commoditiesClient.fetch();
    }

    /**
     * db save + redis put
     * @param commodities
     */
    @Transactional
    public void persistAndCache(Commodities commodities) {
        if (commodities.isEmpty()) {
            log.info("Commodities empty. skip persistence.");
            return;
        }
        try {
            String payloadJson = objectMapper.writeValueAsString(commodities);
            snapshotRepository.save(MarketSnapshot.of(DataType.COMMODITIES, payloadJson));
            cacheService.put(commodities);
            log.info("Commodities snapshot saved. count={}", commodities.prices().size());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize commodities payload", e);
            throw new IllegalStateException("commodities payload serialization failed", e);
        }
    }

    /**
     * Cache-aside fallback — Redis miss 시 DB 최근 스냅샷 deserialize 후 캐시 채움.
     * DB도 empty면 {@link Commodities#empty()} 리턴.
     * deserialize 실패 시 empty로 graceful degradation — 운영 중단 방지.
     */
    private Commodities loadFromDbAndCache() {
        Commodities fromDb = snapshotRepository.findTopByDataTypeOrderBySnapshotAtDesc(DataType.COMMODITIES)
                .map(snapshot -> {
                    try {
                        return objectMapper.readValue(snapshot.getPayload(), Commodities.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize commodities payload. id={}", snapshot.getId(), e);
                        return Commodities.empty();
                    }
                })
                .orElse(Commodities.empty());

        if (!fromDb.isEmpty()) {
            cacheService.put(fromDb);
        }
        return fromDb;
    }
}