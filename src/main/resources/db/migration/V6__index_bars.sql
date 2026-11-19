-- V6__index_bars.sql
-- 지수 종가 시계열 정규화 테이블 (CQRS-lite read model)
--
-- 배경: market_snapshots(payload JSONB, INDICES) 는 이벤트 원본(write model)으로 유지하되,
-- 리베이싱 차트 조회는 이 정규화 테이블을 인덱스 스캔한다. dedup 은 PK(code,date) 로 원천 해결.
-- 같은 (code,date) 재수집 시 ON CONFLICT DO UPDATE 로 최신 수집값이 이긴다(방식 C 원칙 승계).
--
-- 주의: 로컬 마이그레이션 최신 버전을 확인하고 필요 시 파일명 번호를 조정할 것.
-- (메모리상 V4=태그 백필, V5=카테고리 seed 존재. 로컬 실제 번호 기준으로 정렬 유지)

CREATE TABLE index_bars (
    code       VARCHAR(20) NOT NULL,   -- KOSPI | KOSDAQ | NASDAQ_PROXY | SP500_PROXY
    bar_date   DATE        NOT NULL,   -- 영업일
    close      NUMERIC(18, 4) NOT NULL,
    updated_at TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_index_bars PRIMARY KEY (code, bar_date)
);

-- 조회 패턴: 특정 기간(bar_date >= ?) 을 code 별로 date 오름차순. 복합 PK 가 이미
-- (code, bar_date) 순이라 range 스캔 커버. 기간 우선 조회 대비 보조 인덱스 하나 추가.
CREATE INDEX idx_index_bars_date ON index_bars(bar_date);

-- ── 기존 market_snapshots(INDICES) 백필 ──────────────────────────────
-- payload 구조: { "fetchedAt": ..., "series": [ { "code": "...", "bars": [ { "date":"YYYY-MM-DD", "close": n, ... } ] } ] }
-- jsonb_array_elements 로 series → bars 2단 펼침. 같은 (code,date) 다수 등장 시
-- DISTINCT ON 으로 가장 최신 스냅샷(snapshot_at DESC)의 close 만 채택.
INSERT INTO index_bars (code, bar_date, close, updated_at)
SELECT DISTINCT ON (s.code, s.bar_date)
       s.code,
       s.bar_date,
       s.close,
       now()
FROM (
    SELECT
        ms.snapshot_at,
        (series_elem ->> 'code')                     AS code,
        (bar_elem ->> 'date')::date                  AS bar_date,
        (bar_elem ->> 'close')::numeric              AS close
    FROM market_snapshots ms
    CROSS JOIN LATERAL jsonb_array_elements(ms.payload -> 'series') AS series_elem
    CROSS JOIN LATERAL jsonb_array_elements(series_elem -> 'bars')  AS bar_elem
    WHERE ms.data_type = 'INDICES'
      AND (bar_elem ->> 'date') IS NOT NULL
      AND (bar_elem ->> 'close') IS NOT NULL
) AS s
ORDER BY s.code, s.bar_date, s.snapshot_at DESC
ON CONFLICT (code, bar_date) DO NOTHING;
