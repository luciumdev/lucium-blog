package com.ysh.domain.stat.dto;

/**
 * 대시보드 게시글 통계 — 발행/초안 개수 분리.
 */
public record PostStatResponse(
        long published,
        long draft,
        long total
) {
    public static PostStatResponse of(long published, long draft) {
        return new PostStatResponse(published, draft, published + draft);
    }
}