package com.ysh.domain.market.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * KIS 해외 ETF 일별 시세 응답 (NASDAQ 프록시 QQQ, S&P 500 프록시 SPY).
 *
 * Endpoint: {@code GET /uapi/overseas-price/v1/quotations/dailyprice}
 * TR_ID: {@code HHDFS76240000}
 *
 * {@code output1}은 메타(rsym/zdiv/nrec)만 가지고 시계열은 모두 {@code output2}(최신순 100건).
 * 우리는 output2만 매핑.
 *
 * 주의 — KIS는 종목-거래소(EXCD) mismatch 시 silent fail: {@code rt_cd:"0"}이지만
 * {@code output2}가 빈 배열. QQQ는 EXCD=NAS, SPY는 EXCD=AMS로 정확히 박아야.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisOverseasIndexResponse(
        @JsonProperty("rt_cd") String rtCd,
        @JsonProperty("msg_cd") String msgCd,
        @JsonProperty("msg1") String msg1,
        @JsonProperty("output2") List<Output2> output2
) {

    /**
     * 일별 시계열 1건.
     * {@code changeRate}는 부호 prefix 포함 ({@code "+1.17"}, {@code "-1.17"}) — Java
     * {@code Double.parseDouble}이 그대로 처리. {@code change}는 prefix 유무 명시 없음 →
     * {@code sign}으로 안전 보정.
     * @param date  YYYYMMDD
     * @param close
     * @param sign 1상한 2상승 3보합 4하한 5하락
     * @param change 전일 대비
     * @param changeRate 등락률 % "+1.17" 부호 포함
     * @param openPrice
     * @param highPrice
     * @param lowPrice
     * @param volume
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output2(
            @JsonProperty("xymd") String date,
            @JsonProperty("clos") String close,
            @JsonProperty("sign") String sign,
            @JsonProperty("diff") String change,
            @JsonProperty("rate") String changeRate,
            @JsonProperty("open") String openPrice,
            @JsonProperty("high") String highPrice,
            @JsonProperty("low") String lowPrice,
            @JsonProperty("tvol") String volume
    ) {
    }
}