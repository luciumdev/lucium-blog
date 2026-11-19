package com.ysh.domain.collection.dto;

import java.util.List;

/**
 * /status 작업별 일간 요약.
 * @param days 조회 일수 (오늘 포함)
 */
public record CollectionDailyResponse(
        int days,
        List<JobDaily> jobs
) {
    /**
     * @param successRate   기간 성공률(%) — EMPTY 제외. 분모 0 이면 null
     * @param avgDurationMs 평균 소요(ms). 실행이 없으면 null
     * @param daily         과거 → 오늘
     */
    public record JobDaily(
            String job,
            String label,
            String cycle,
            int totalRuns,
            Double successRate,
            Long avgDurationMs,
            List<Day> daily
    ) {}

    /** @param date "yyyy-MM-dd" */
    public record Day(
            String date,
            int success,
            int partial,
            int empty,
            int failed
    ) {}
}
