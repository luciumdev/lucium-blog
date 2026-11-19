-- V9__drop_llm_briefings.sql
-- LLM 시장 브리핑 기능 제거 (2026-10)
-- 사유: 뉴스·마켓 페이지가 "수집 데이터 + ES 집계" 중심으로 바뀌면서 브리핑의 역할이 사라짐.
--       2시간마다 Sonnet 호출하던 비용도 함께 제거.
-- 과거 브리핑 텍스트가 필요하면 이 마이그레이션 적용 전에 백업할 것 (되돌릴 수 없음).

DROP TABLE IF EXISTS llm_briefings;
