package com.ysh.domain.market.domain;

import java.time.LocalDate;

/**
 *지수 일별 봉
 * @param date    영업일
 * @param open    시가
 * @param high    고가
 * @param low     저가
 * @param close   종가
 * @param change  전일 대비 (부호 포함)
 * @param changeRate 등락률 % (부호 포함, 예: -1.17)
 * @param volume   거래량
 */
public record IndexBar(
        LocalDate date,
        double open,
        double high,
        double low,
        double close,
        double change,
        double changeRate,
        long volume
) {
}
