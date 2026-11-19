package com.ysh.domain.market.repository;

import com.ysh.domain.market.domain.DataType;
import com.ysh.domain.market.entity.MarketSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MarketSnapshotRepository extends JpaRepository<MarketSnapshot, Long> {
    Optional<MarketSnapshot> findTopByDataTypeOrderBySnapshotAtDesc(DataType dataType);

    /**
     * 최신 snapshot
     * @param dataType
     * @param snapshotAt
     * @return
     */
    Optional<MarketSnapshot> findTopByDataTypeAndSnapshotAtBeforeOrderBySnapshotAtDesc(DataType dataType, LocalDateTime snapshotAt);

    /**
     * 통화별 일 마감 환율 — 날짜마다 그날 마지막 FOREX 스냅샷의 매매기준율.
     * payload(JSONB) 의 rates 배열을 펼쳐 currencyCode 로 필터. 날짜 오름차순.
     * ('::' 캐스트는 네이티브 쿼리 파라미터 파싱과 충돌할 수 있어 CAST 사용)
     */
    @Query(value = """
            SELECT DISTINCT ON (CAST(s.snapshot_at AS DATE))
                   CAST(s.snapshot_at AS DATE)                   AS day,
                   CAST(r ->> 'dealBasR' AS DOUBLE PRECISION)   AS rate
              FROM market_snapshots s
             CROSS JOIN LATERAL jsonb_array_elements(s.payload -> 'rates') AS r
             WHERE s.data_type = 'FOREX'
               AND s.snapshot_at >= :from
               AND r ->> 'currencyCode' = :currency
             ORDER BY CAST(s.snapshot_at AS DATE), s.snapshot_at DESC
            """, nativeQuery = true)
    List<DailyRate> findDailyClosingRates(@Param("from") LocalDateTime from,
                                          @Param("currency") String currency);

    interface DailyRate {
        LocalDate getDay();
        double getRate();
    }
}
