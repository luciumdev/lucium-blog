-- ─────────────────────────────────────────────────────────────
-- V4: tags.post_count 백필
--   · 배선(PostService attach/sync/delete)은 신규 변경분만 반영
--   · 기존 데이터는 이 마이그레이션으로 실제 연결 수 기준 재계산
--   · 정의1(전체 글 기준, DRAFT 포함) — posts 상태 필터 없이 post_tags 전체 카운트
--   · 절대값 대입이라 멱등(재실행 안전)
-- ─────────────────────────────────────────────────────────────
UPDATE tags t
SET post_count = COALESCE(
    (SELECT COUNT(*) FROM post_tags pt WHERE pt.tag_id = t.id),
    0
);
