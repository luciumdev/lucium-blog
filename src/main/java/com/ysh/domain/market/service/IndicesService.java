package com.ysh.domain.market.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.api.market.dto.IndicesHistoryResponse;
import com.ysh.common.cache.CacheResult;
import com.ysh.domain.market.client.kis.KisDomesticIndicesClient;
import com.ysh.domain.market.client.kis.KisOverseasIndicesClient;
import com.ysh.domain.market.domain.*;
import com.ysh.domain.market.entity.MarketSnapshot;
import com.ysh.domain.market.repository.IndexBarRepository;
import com.ysh.domain.market.repository.MarketSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 4개 시장 지수 (KOSPI, KOSDAQ, NASDAQ_PROXY, SP500_PROXY) 통합 수집 + 캐시 + DB.
 *
 * <p>호출 정책:
 * <ul>
 *   <li>4개 지수를 직렬 호출. 호출 사이 1.1s 대기로 KIS 모의 환경 초당 1회 한도(EGW00201) 회피.</li>
 *   <li>병렬 호출 NO — 동시 호출 시 KIS rate limit 위반.</li>
 *   <li>부분 실패 허용 — 일부 지수 fetch 실패해도 IndexSeries.empty()로 자리 채워 저장.</li>
 * </ul>
 *
 * <p>책임 분리:
 * <ul>
 *   <li>{@link #fetchAll()} — 외부 KIS API만 호출 (트랜잭션 밖, ~5초)</li>
 *   <li>{@link #persistAndCache(IndicesData)} — DB save + index_bars upsert + Redis put (트랜잭션 안)</li>
 *   <li>orchestration은 호출자(MarketDataScheduler)가 책임</li>
 * </ul>
 *
 * <p>저장 모델 — CQRS-lite:
 * <ul>
 *   <li>write model: {@code market_snapshots}(JSONB payload) — 이벤트 원본 보관</li>
 *   <li>read model: {@code index_bars}(정규화, PK(code,date)) — 시계열 차트 조회 전용</li>
 * </ul>
 *
 * <p>향후 — 두 번째 프로젝트에서 Resilience4j RateLimiter로 throttle 정교화.
 */
@Slf4j
@Service
public class IndicesService {

    /** KIS 모의 환경 시세 조회 한도(약 1초당 1회) 회피용 호출 간 대기. */
    private static final long INTER_CALL_DELAY_MS = 1100L;

    /** 히스토리 조회 기간 상한(일). */
    private static final int MAX_HISTORY_DAYS = 365;

    private final KisDomesticIndicesClient domesticClient;
    private final KisOverseasIndicesClient overseasClient;
    private final IndicesCacheService cacheService;
    private final MarketSnapshotRepository snapshotRepository;
    private final IndexBarRepository indexBarRepository;
    private final ObjectMapper objectMapper;

    public IndicesService(
            KisDomesticIndicesClient domesticClient,
            KisOverseasIndicesClient overseasClient,
            IndicesCacheService cacheService,
            MarketSnapshotRepository snapshotRepository,
            IndexBarRepository indexBarRepository,
            ObjectMapper objectMapper
    ) {
        this.domesticClient = domesticClient;
        this.overseasClient = overseasClient;
        this.cacheService = cacheService;
        this.snapshotRepository = snapshotRepository;
        this.indexBarRepository = indexBarRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void persistAndCache(IndicesData data) {
        long nonEmpty = data.series().stream().filter(s -> !s.isEmpty()).count();
        if (nonEmpty == 0) {
            log.warn("All indices empty — skip persistence (stale cache/DB preserved)");
            return;
        }
        try {
            String payloadJson = objectMapper.writeValueAsString(data);
            snapshotRepository.save(MarketSnapshot.of(DataType.INDICES, payloadJson));

            // read model 정규화 테이블 upsert (같은 트랜잭션 — 원자적).
            // payload 안 bars 를 (code, date, close) 로 펼쳐 upsert.
            // 같은 (code,date) 는 PK 제약 + ON CONFLICT 로 최신값이 이김.
            upsertIndexBars(data);

            cacheService.put(data);
            log.info("Indices snapshot saved + index_bars upserted. nonEmpty={}/{}",
                    nonEmpty, data.series().size());
        } catch (Exception e) {
            log.error("Failed to persist indices payload", e);
            throw new IllegalStateException("indices persist failed", e);
        }
    }

    /**
     * 스냅샷 payload 의 각 지수 bars 를 index_bars 로 upsert.
     * 빈 시리즈/날짜 null bar 는 건너뜀.
     */
    private void upsertIndexBars(IndicesData data) {
        for (IndexSeries series : data.series()) {
            if (series.isEmpty()) continue;
            String code = series.code().name();
            for (IndexBar bar : series.bars()) {
                if (bar.date() == null) continue;
                indexBarRepository.upsert(code, bar.date(), bar.close());
            }
        }
    }

    /**
     * Controller에서 X-Cache 헤더 결정용 — hit/miss 시그널까지 동봉.
     */
    public CacheResult<IndicesData> getLatestCacheable() {
        return cacheService.get()
                .map(CacheResult::hit)
                .orElseGet(() -> CacheResult.miss(loadFromDbAndCache()));
    }

    /**
     * 지수 시계열 히스토리 — 정규화 테이블(index_bars) 인덱스 스캔.
     *
     * <p>dedup 은 write path 의 PK(code,date) upsert 에서 이미 끝나 있어, 조회는
     * range 스캔 + code 별 그룹핑뿐. JSONB 역직렬화 없음 → 데이터 증가에도 조회 비용 안정.
     *
     * @param days 조회 기간(일). 1~365 클램프.
     */
    @Transactional(readOnly = true)
    public IndicesHistoryResponse getHistory(int days) {
        int clamped = Math.max(1, Math.min(days, MAX_HISTORY_DAYS));
        LocalDate fromDate = LocalDate.now().minusDays(clamped);

        List<IndexBarRepository.IndexBarPoint> rows =
                indexBarRepository.findPointsFrom(fromDate);

        // rows 는 code, bar_date 오름차순. code 별로 순서 유지하며 그룹핑.
        Map<String, List<IndicesHistoryResponse.Point>> grouped = new LinkedHashMap<>();
        for (IndexBarRepository.IndexBarPoint row : rows) {
            grouped.computeIfAbsent(row.getCode(), k -> new ArrayList<>())
                    .add(new IndicesHistoryResponse.Point(row.getBarDate(), row.getClose()));
        }

        List<IndicesHistoryResponse.SeriesPoints> series = new ArrayList<>();
        grouped.forEach((code, points) ->
                series.add(new IndicesHistoryResponse.SeriesPoints(
                        code, displayNameOf(code), points)));

        return new IndicesHistoryResponse(fromDate, series);
    }

    /** code 문자열 → displayName. 알 수 없는 code 는 그대로 폴백. */
    private String displayNameOf(String code) {
        try {
            return IndexCode.valueOf(code).displayName();
        } catch (IllegalArgumentException e) {
            return code;
        }
    }

    /**
     * 4개 지수 직렬 fetch. KIS rate limit 회피 위해 호출 사이 1.1s sleep.
     */
    public IndicesData fetchAll() {
        LocalDateTime startedAt = LocalDateTime.now();
        IndexCode[] codes = IndexCode.values();
        List<IndexSeries> series = new ArrayList<>(codes.length);

        for (int i = 0; i < codes.length; i++) {
            IndexCode code = codes[i];
            IndexSeries result = switch (code.market()) {
                case DOMESTIC -> domesticClient.fetch(code);
                case OVERSEAS -> overseasClient.fetch(code);
            };
            series.add(result);

            // 마지막 호출 후엔 대기 불필요
            if (i < codes.length - 1) {
                throttle();
            }
        }
        return new IndicesData(startedAt, series);
    }

    /**
     * KIS 호출 간 대기. interrupt는 스케줄러 종료 시그널 — propagate 후 예외.
     */
    private static void throttle() {
        try {
            Thread.sleep(INTER_CALL_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Indices fetch throttle interrupted", e);
        }
    }

    private IndicesData loadFromDbAndCache() {
        IndicesData fromDb = snapshotRepository
                .findTopByDataTypeOrderBySnapshotAtDesc(DataType.INDICES)
                .map(snapshot -> {
                    try {
                        return objectMapper.readValue(snapshot.getPayload(), IndicesData.class);
                    } catch (Exception e) {
                        log.error("Failed to deserialize indices payload. id={}", snapshot.getId(), e);
                        return IndicesData.empty();
                    }
                })
                .orElse(IndicesData.empty());

        if (!fromDb.series().isEmpty()) {
            cacheService.put(fromDb);
        }
        return fromDb;
    }

}