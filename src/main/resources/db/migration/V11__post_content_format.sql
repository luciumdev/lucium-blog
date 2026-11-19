-- 게시글 본문 형식: MARKDOWN(에디터 작성) / HTML(원본 파일 그대로 게시)
-- 기존 글은 전부 마크다운이므로 DEFAULT 로 채움
ALTER TABLE posts
    ADD COLUMN content_format VARCHAR(10) NOT NULL DEFAULT 'MARKDOWN';

ALTER TABLE posts
    ADD CONSTRAINT chk_posts_content_format
        CHECK (content_format IN ('MARKDOWN', 'HTML'));
