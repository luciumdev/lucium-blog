-- V3__ai_journal_schema.sql
-- AI 저널 날짜별 아카이브 테이블

-- 일일 브리핑 (날짜별 1건, 영구 보관)
CREATE TABLE ai_journal_briefings (
                                      id            BIGSERIAL PRIMARY KEY,
                                      journal_date  DATE      NOT NULL,
                                      daily_summary TEXT      NOT NULL,
                                      items_json    JSONB     NOT NULL,   -- AiNewsItem[] 직렬화 저장
                                      top_keywords  TEXT[]    NOT NULL DEFAULT '{}',
                                      total_count   INT       NOT NULL DEFAULT 0,
                                      high_count    INT       NOT NULL DEFAULT 0,
                                      medium_count  INT       NOT NULL DEFAULT 0,
                                      low_count     INT       NOT NULL DEFAULT 0,
                                      created_at    TIMESTAMP NOT NULL DEFAULT now(),
                                      CONSTRAINT uq_ai_journal_date UNIQUE (journal_date)
);
CREATE INDEX idx_ai_journal_date ON ai_journal_briefings(journal_date DESC);

-- 수집 원문 (날짜별 + 소스별 + 중요도별 필터 지원)
CREATE TABLE ai_news_items (
                               id           BIGSERIAL     PRIMARY KEY,
                               journal_date DATE          NOT NULL,
                               source       VARCHAR(50)   NOT NULL,
                               title        VARCHAR(500)  NOT NULL,
                               summary      TEXT,
                               important    VARCHAR(10)   NOT NULL,   -- HIGH | MEDIUM | LOW
                               category     VARCHAR(30)   NOT NULL,
                               tags         TEXT[]        NOT NULL DEFAULT '{}',
                               url          VARCHAR(1000) NOT NULL,
                               published_at TIMESTAMP,
                               fetched_at   TIMESTAMP     NOT NULL DEFAULT now(),
                               CONSTRAINT uq_ai_news_url UNIQUE (url),
                               CONSTRAINT chk_ai_news_important CHECK (important IN ('HIGH', 'MEDIUM', 'LOW'))
);
CREATE INDEX idx_ai_news_date_source   ON ai_news_items(journal_date DESC, source);
CREATE INDEX idx_ai_news_date_important ON ai_news_items(journal_date DESC, important);