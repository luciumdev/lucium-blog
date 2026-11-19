package com.ysh.domain.post.entity;

/**
 * 게시글 본문 형식.
 * MARKDOWN — 에디터(MDXEditor)로 작성, react-markdown 으로 렌더
 * HTML     — 직접 만든 HTML 파일을 그대로 게시, 프론트에서 격리된 iframe(sandbox)으로 렌더
 */
public enum ContentFormat {
    MARKDOWN,
    HTML
}
