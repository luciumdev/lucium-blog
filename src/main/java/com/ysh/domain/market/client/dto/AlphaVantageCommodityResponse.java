package com.ysh.domain.market.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 *
 * @param name
 * @param interval
 * @param unit
 * @param data -> data.value: String — "." 또는 "N/A" 가능 (휴장일)
 */
public record AlphaVantageCommodityResponse(
        String name,
        String interval,
        String unit,
        List<DataPoint> data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DataPoint(
            String date,
            String value
    ) {}
}
