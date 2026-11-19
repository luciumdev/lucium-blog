package com.ysh.api.market.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 지수 시계열 히스토리 응답 (스냅샷 병합 결과).
 *
 * <p>여러 {@code MarketSnapshot(INDICES)} 의 bars 를 지수별로 병합·날짜 중복 제거해
 * 과거→최신 순 종가 시계열로 평탄화한 형태. 프론트 {@code IndexTimeSeries} 와 1:1 대응하여
 * 클라이언트는 필드 매핑만 하면 리베이싱 차트에 바로 투입 가능.
 *
 * @param from   조회 시작 기준일 (요청 days 로 계산된 하한)
 * @param series 지수별 종가 시계열
 */
public record IndicesHistoryResponse(
        LocalDate from,
        List<SeriesPoints> series
) {

    public record SeriesPoints(
            String code,          // KOSPI, KOSDAQ, NASDAQ_PROXY, SP500_PROXY
            String displayName,
            List<Point> points    // 과거 → 최신 오름차순
    ) {
    }

    public record Point(
            LocalDate date,
            double close
    ) {
    }
}