package com.ysh.infra.search.news.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 키워드 일별 기사 수 (과거 → 오늘, 빈 날은 0).
 */
public record KeywordTrendResponse(
        String keyword,
        List<DailyCount> series
) {
    public KeywordTrendResponse {
        series = series == null ? List.of() : List.copyOf(series);
    }

    public static KeywordTrendResponse zeros(String keyword, LocalDate from, LocalDate to) {
        List<DailyCount> series = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            series.add(new DailyCount(d, 0));
        }
        return new KeywordTrendResponse(keyword, series);
    }

    public record DailyCount(LocalDate date, long count) {}
}
