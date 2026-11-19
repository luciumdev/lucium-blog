-- V8__news_enrichment.sql
-- 뉴스 보강(enrichment) 컬럼: 수집 직후 LLM 배치로 채움
--   title_ko        : HackerNews 영어 제목의 한국어 번역 (경제 뉴스는 NULL)
--   keywords        : 정규화된 주제 키워드 JSON 배열 (예: ["반도체","엔비디아"]) — 테크·경제 교차 집계 기준
--   enriched_at     : 보강 완료 시각. NULL = 대기
--   enrich_attempts : 실패 누적 횟수. 상한 초과 시 재시도 대상에서 제외 (무한 재시도 방지)

ALTER TABLE news_feeds
    ADD COLUMN title_ko        VARCHAR(500),
    ADD COLUMN keywords        JSONB,
    ADD COLUMN enriched_at     TIMESTAMP,
    ADD COLUMN enrich_attempts INTEGER NOT NULL DEFAULT 0;

-- 대기 건 조회 전용 부분 인덱스 (보강 완료 행은 인덱스에서 빠짐)
CREATE INDEX idx_news_feeds_enrich_pending
    ON news_feeds (published_at DESC)
    WHERE enriched_at IS NULL;
