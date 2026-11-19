package com.ysh.domain.market.repository;

import com.ysh.domain.market.entity.IndexBarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * index_bars 정규화 테이블 접근 (CQRS-lite read model).
 *
 * <p>write 는 ON CONFLICT upsert 네이티브 쿼리(재수집 시 최신값 우선 = 방식 C 원칙),
 * read 는 projection 으로 code 별 (date, close) 평탄화 행을 과거→최신 순 반환.
 */
@Repository
public interface IndexBarRepository extends JpaRepository<IndexBarEntity, IndexBarEntity.Pk> {

    /**
     * (code, bar_date) upsert. 스냅샷 저장과 같은 트랜잭션에서 호출.
     * 같은 (code,date) 재수집 시 close 를 최신값으로 갱신.
     */
    @Modifying
    @Query(value = """
            INSERT INTO index_bars (code, bar_date, close, updated_at)
            VALUES (:code, :barDate, :close, now())
            ON CONFLICT (code, bar_date)
            DO UPDATE SET close = EXCLUDED.close, updated_at = now()
            """, nativeQuery = true)
    void upsert(@Param("code") String code,
                @Param("barDate") LocalDate barDate,
                @Param("close") double close);

    /**
     * 기준일 이후 종가 시계열 — code, bar_date 오름차순.
     * 리베이싱 차트 전제(과거→최신)에 맞춰 정렬해서 반환.
     */
    @Query(value = """
            SELECT code AS code, bar_date AS barDate, close AS close
            FROM index_bars
            WHERE bar_date >= :from
            ORDER BY code, bar_date ASC
            """, nativeQuery = true)
    List<IndexBarPoint> findPointsFrom(@Param("from") LocalDate from);

    /** 조회 projection — code 별 (date, close) 평탄화 행. */
    interface IndexBarPoint {
        String getCode();
        LocalDate getBarDate();
        double getClose();
    }
}