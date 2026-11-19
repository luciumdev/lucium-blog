package com.ysh.api.market.dto;

import com.ysh.domain.market.domain.Commodities;
import com.ysh.domain.market.domain.CommodityPrice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * /v1/market/commodities 응답.
 *
 * code는 enum.name() String 직렬화 — 프론트에서 "GOLD"/"SILVER"/"WTI"/"COPPER" 로 받음.
 */
public record CommoditiesResponse(
        LocalDateTime snapshotAt,
        List<CommodityPriceResponse> prices
) {
    public static CommoditiesResponse from(Commodities commodities) {
        return new CommoditiesResponse(
                commodities.snapshotAt(),
                commodities.prices().stream()
                        .map(CommodityPriceResponse::from)
                        .toList()
        );
    }

    public record CommodityPriceResponse(
            String code,
            String name,
            BigDecimal price,
            BigDecimal changeValue,
            BigDecimal changePercent,
            String currency,
            String unit
    ) {
        public static CommodityPriceResponse from(CommodityPrice p) {
            return new CommodityPriceResponse(
                    p.code().name(),
                    p.name(),
                    p.price(),
                    p.changeValue(),
                    p.changePercent(),
                    p.currency(),
                    p.unit()
            );
        }
    }
}