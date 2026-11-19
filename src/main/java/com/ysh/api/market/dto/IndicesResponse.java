package com.ysh.api.market.dto;

import com.ysh.domain.market.domain.IndexBar;
import com.ysh.domain.market.domain.IndexSeries;
import com.ysh.domain.market.domain.IndicesData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 4개 지수 시계열 응답.
 *
 * <p>{@link ForexResponse} 패턴 — nested record로 평탄화. 도메인 record를 직접 노출하지 않고
 * 응답 전용 DTO로 변환해서 도메인 모델 변경이 API에 새지 않게.
 */
public record IndicesResponse(
        LocalDateTime fetchedAt,
        List<SeriesResponse> series
) {

    public static IndicesResponse from(IndicesData data) {
        return new IndicesResponse(
                data.fetchedAt(),
                data.series().stream()
                        .map(SeriesResponse::from)
                        .toList()
        );
    }

    /**
     * 단일 지수의 시계열. {@code latest}는 빈 시리즈일 때 null 가능.
     */
    public record SeriesResponse(
            String code,           // KOSPI, KOSDAQ, NASDAQ_PROXY, SP500_PROXY
            String displayName,    // "코스피", "나스닥(QQQ ETF)", ...
            BarResponse latest,
            List<BarResponse> bars
    ) {
        public static SeriesResponse from(IndexSeries s) {
            return new SeriesResponse(
                    s.code().name(),
                    s.code().displayName(),
                    s.latest() != null ? BarResponse.from(s.latest()) : null,
                    s.bars().stream().map(BarResponse::from).toList()
            );
        }
    }

    public record BarResponse(
            LocalDate date,
            double open,
            double high,
            double low,
            double close,
            double change,
            double changeRate,
            long volume
    ) {
        public static BarResponse from(IndexBar b) {
            return new BarResponse(
                    b.date(),
                    b.open(),
                    b.high(),
                    b.low(),
                    b.close(),
                    b.change(),
                    b.changeRate(),
                    b.volume()
            );
        }
    }
}