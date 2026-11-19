-- V2__market_schema.sql
-- 마켓 대시보드 관련 테이블

CREATE TABLE market_snapshots (
                                  id          BIGSERIAL   PRIMARY KEY,
                                  snapshot_at TIMESTAMP   NOT NULL DEFAULT now(),
                                  data_type   VARCHAR(30) NOT NULL,  -- INDICES | FOREX | COMMODITIES
                                  payload     JSONB       NOT NULL
);
CREATE INDEX idx_market_snapshots_type_at ON market_snapshots(data_type, snapshot_at DESC);

-- 뉴스 피드 (마켓 경제/IT 뉴스, AI 뉴스 통합 사용)
-- category: ECONOMY | IT | AI
CREATE TABLE news_feeds (
                            id           BIGSERIAL     PRIMARY KEY,
                            source       VARCHAR(50)   NOT NULL,
                            category     VARCHAR(20)   NOT NULL,
                            title        VARCHAR(500)  NOT NULL,
                            url          VARCHAR(1000) NOT NULL,
                            summary      TEXT,
                            published_at TIMESTAMP     NOT NULL,
                            fetched_at   TIMESTAMP     NOT NULL DEFAULT now(),
                            CONSTRAINT uq_news_url UNIQUE (url)
);
CREATE INDEX idx_news_feeds_category_published ON news_feeds(category, published_at DESC);

CREATE TABLE llm_briefings (
                               id         BIGSERIAL PRIMARY KEY,
                               briefing   TEXT      NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT now()
);