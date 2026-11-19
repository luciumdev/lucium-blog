package com.ysh.domain.stat.dto;

/**
 * 메인 프로필 카드용 공개 통계.
 *
 * @param publishedPosts 발행 글 수
 * @param todayVisitors  오늘(KST) 순 방문자
 * @param totalVisitors  누적 순 방문자
 */
public record SiteStatResponse(
        long publishedPosts,
        long todayVisitors,
        long totalVisitors
) {
}
