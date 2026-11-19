package com.ysh.domain.market.domain;

import java.math.BigDecimal;

/**
 * 단일 원자재 가격 스냅샷
 * @param code
 * @param name
 * @param price 현재가
 * @param changePercent 전일 대비 변동액 (음수O)
 * @param unit
 */
public record CommodityPrice(
        CommodityCode code,
        String name,
        BigDecimal price,
        BigDecimal changeValue,
        BigDecimal changePercent,
        String currency,
        String unit
) {}
