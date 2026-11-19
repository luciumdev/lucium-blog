-- V7__alter_index_bars_close_double.sql
-- index_bars.close 타입 정정: NUMERIC(18,4) → DOUBLE PRECISION
--
-- 사유: 엔티티 IndexBarEntity.close 가 double 이라 Hibernate 는 float(53)=double precision 을
-- 기대하는데, V6 는 NUMERIC 으로 생성해 ddl-auto=validate 스키마 검증이 실패했다
-- (found numeric, expecting float(53)). 도메인 IndexBar.close 도 double 이라 타입을
-- double 로 통일하는 게 일관적(지수 종가는 차트 표시용 — BigDecimal 정밀도 불필요).
--
-- V6 는 이미 적용 완료라 수정 불가(체크섬 검증). 새 버전으로 ALTER.
-- 기존 데이터는 numeric → double precision 자동 캐스팅되어 안전(멱등적 성격).

ALTER TABLE index_bars
    ALTER COLUMN close TYPE DOUBLE PRECISION USING close::double precision;
