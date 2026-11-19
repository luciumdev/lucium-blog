package com.ysh.domain.market.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @param result 1:성공, 2:data오류, 3:인증오류 4:한도초과
 * @param curUnit usd, jpy, ...
 * @param curNm "미국 달러" ..
 * @param dealBasR "1,234.56"
 */
public record KoreaEximResponse(
   @JsonProperty("result") int result,
   @JsonProperty("cur_unit") String curUnit,
   @JsonProperty("cur_nm") String curNm,
   @JsonProperty("deal_bas_r") String dealBasR

) {}
