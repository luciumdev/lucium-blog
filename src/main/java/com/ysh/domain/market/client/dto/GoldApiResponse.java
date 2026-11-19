package com.ysh.domain.market.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {
 *   "metal": "XAU",
 *   "currency": "USD",
 *   "price": 4665.825,
 *   "ch": -27.2,
 *   "chp": -0.58,
 *   "prev_close_price": 4693.025,
 *   "timestamp": 1777005765
 * }
 */
public record GoldApiResponse(
        String metal,
        String currency,
        double price,
        double ch,
        double chp,
        @JsonProperty("prev_close_price") double prevClosePrice
) {}
