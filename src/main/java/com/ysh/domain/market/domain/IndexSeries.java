package com.ysh.domain.market.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * 단일 지수의 시계열.
 *
 * {@code bars}는 시간 역순 — bars[0]이 가장 최근 영업일. {@code latest}는 bars[0]의 alias로
 * 호출 측 편의. 빈 응답이면 {@link #empty(IndexCode)} 사용.
 *
 * @param code   지수 식별자
 * @param latest 가장 최근 봉 (bars[0]과 동일). 빈 시계열이면 null.
 * @param bars   시계열. 빈 리스트 가능.
 */
public record IndexSeries(
        IndexCode code,
        IndexBar latest,
        List<IndexBar> bars
) {

    public static IndexSeries empty(IndexCode code) {
        return new IndexSeries(code, null, List.of());
    }
    @JsonIgnore
    public boolean isEmpty() {
        return bars == null || bars.isEmpty();
    }
}
