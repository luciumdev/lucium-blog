package com.ysh.domain.market.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 원자재 스냅샷 묶음  -> JSON 캐시+ DB jsonb 페이로드 단위
 * @param snapshotAt
 * @param prices
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Commodities(
        LocalDateTime snapshotAt,
        List<CommodityPrice> prices
) {
    public static Commodities empty() {
        return new Commodities(LocalDateTime.now(), List.of());
    }
    @JsonIgnore
    public boolean isEmpty() { return prices == null || prices.isEmpty(); }

}
