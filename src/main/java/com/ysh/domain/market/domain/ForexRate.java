package com.ysh.domain.market.domain;

/**
 *
 * @param currencyCode USD, EUR, JPY, ...
 * @param currencyName 미국 달러
 * @param dealBasR 매매기준율
 */
public record ForexRate(
    String currencyCode,
    String currencyName,
    double dealBasR
){}
