package com.ysh.domain.admin.dto;

/**
 * 대시보드 게시글 통계.
 * published / draft 분리해 전달한다.
 */
public record PostStatsResponse(
        long published,
        long draft,
        long total
) {
    public static PostStatsResponse of(long published, long draft) {
        return new PostStatsResponse(published, draft, published + draft);
    }
}