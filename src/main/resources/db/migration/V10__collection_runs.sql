-- V10__collection_runs.sql
-- 스케줄러 실행 기록 — 메인 "데이터 수집" 카드, 추후 /status 페이지의 원천 데이터.
--   job          : NEWS | FOREX | INDICES | AI_JOURNAL | COMMODITIES
--   trigger_type : SCHEDULED | BOOTSTRAP
--   status       : SUCCESS | PARTIAL(일부 실패) | EMPTY(정상이지만 데이터 없음 — 주말 환율 등) | FAILED
--   item_count   : 이번 실행에서 새로 저장된 건수
-- 하루 약 80행. 90일 지난 행은 CollectionRunRetention 이 매일 삭제.

CREATE TABLE collection_runs (
    id           BIGSERIAL    PRIMARY KEY,
    job          VARCHAR(30)  NOT NULL,
    trigger_type VARCHAR(20)  NOT NULL,
    status       VARCHAR(10)  NOT NULL,
    started_at   TIMESTAMP    NOT NULL,
    finished_at  TIMESTAMP    NOT NULL,
    item_count   INTEGER      NOT NULL DEFAULT 0,
    message      VARCHAR(500),
    CONSTRAINT chk_collection_runs_status CHECK (status IN ('SUCCESS', 'PARTIAL', 'EMPTY', 'FAILED'))
);
CREATE INDEX idx_collection_runs_job_started ON collection_runs (job, started_at DESC);
CREATE INDEX idx_collection_runs_started     ON collection_runs (started_at DESC);
