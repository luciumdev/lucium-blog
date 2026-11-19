package com.ysh.domain.market.domain;

import java.time.LocalDateTime;
import java.util.List;
/**
 * 4개 지수(KOSPI, KOSDAQ, NASDAQ_PROXY, SP500_PROXY) 시계열 묶음.
 *
 *  부분 실패 허용 — 일부 지수만 fetch 실패해도 나머지는 정상 반환되도록 series 안에
 * {@link IndexSeries#empty(IndexCode)}가 섞일 수 있음. 호출 측에서 {@link IndexSeries#isEmpty()}로 확인.
 *
 * @param fetchedAt 수집 시각
 * @param series    4개 지수의 시계열 (실패한 항목도 자리 채움)
 */
public record IndicesData(
        LocalDateTime fetchedAt,
        List<IndexSeries> series
) {

    public static IndicesData empty() {
        return new IndicesData(LocalDateTime.now(), List.of());
    }
}
