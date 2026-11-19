package com.ysh.domain.market.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
/**
 * KIS 국내 일자별 지수 시세 응답.
 *
 * Endpoint: {@code GET /uapi/domestic-stock/v1/quotations/inquire-index-daily-price}
 * TR_ID: {@code FHPUP02120000}
 *
 * {@code output1}은 요약(현재 시점)이고 {@code output2}는 시계열(최신순 ~100건).
 * output2[0]가 그날의 종가/시고저/등락률을 포함하므로 우리는 output2만 매핑하고 output1은 무시.
 *
 * 주의 — debugging trap: {@code rt_cd:"0"}이지만 {@code output2}가 빈 배열로 올 수 있음
 * (잘못된 ISCD 등). 호출 측에서 명시 체크.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KisDomesticIndexResponse(
        @JsonProperty("rt_cd") String rtCd,
        @JsonProperty("msg_cd") String msgCd,
        @JsonProperty("msg1") String msg1,
        @JsonProperty("output2") List<Output2> output2
) {
    /**
     *
     * 일별 시계열 1건.
     *
     * {@code changeRate}는 절대값일 가능성 — {@code prdy_vrss_sign}(1상한 2상승 3보합 4하한 5하락)으로
     * 부호 적용 필요. {@code change}도 동일하게 sign 우선 적용.
     * @param date YYYYMMDD
     * @param closePrice 종가
     * @param change 전일 대비
     * @param sign 1상한 2상승 3보합 4하한 5하락
     * @param changeRate  등락률 % (숫자만, prefix X)
     * @param openPrice
     * @param highPrice
     * @param lowPrice
     * @param volume
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Output2(
            @JsonProperty("stck_bsop_date") String date,
            @JsonProperty("bstp_nmix_prpr") String closePrice,
            @JsonProperty("bstp_nmix_prdy_vrss") String change,
            @JsonProperty("prdy_vrss_sign") String sign,
            @JsonProperty("bstp_nmix_prdy_ctrt") String changeRate,
            @JsonProperty("bstp_nmix_oprc") String openPrice,
            @JsonProperty("bstp_nmix_hgpr") String highPrice,
            @JsonProperty("bstp_nmix_lwpr") String lowPrice,
            @JsonProperty("acml_vol") String volume
    ) {
    }
}