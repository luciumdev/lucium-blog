package com.ysh.domain.market.client.kis;

import com.ysh.domain.market.client.dto.KisDomesticIndexResponse;
import com.ysh.domain.market.domain.IndexBar;
import com.ysh.domain.market.domain.IndexCode;
import com.ysh.domain.market.domain.IndexSeries;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KIS 국내 지수 (KOSPI, KOSDAQ) 일별 시계열 클라이언트.
 *
 * Endpoint: {@code GET /uapi/domestic-stock/v1/quotations/inquire-index-daily-price}
 * TR_ID: {@code FHPUP02120000} (모의/실전 동일)
 *
 * 응답 output2는 최신순 ~100일치 시계열. seriesDepth만큼 take.
 *
 * 실패 정책 — 4xx/5xx, 네트워크 에러, rt_cd 비정상, 빈 output2 모두 {@link IndexSeries#empty(IndexCode)}로 폴백.
 */
@Slf4j
@Component
public class KisDomesticIndicesClient {
    private static final String PATH = "/uapi/domestic-stock/v1/quotations/inquire-index-daily-price";
    private static final String TR_ID = "FHPUP02120000";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final RestClient restClient;
    private final KisAuthService kisAuthService;
    private final String appKey;
    private final String appSecret;
    private final int seriesDepth;


    public KisDomesticIndicesClient(@Qualifier("kisRestClient") RestClient restClient,
                                    KisAuthService kisAuthService,
                                    @Value("${market.kis.app-key}") String appKey,
                                    @Value("${market.kis.app-secret}") String appSecret,
                                    @Value("${market.kis.indices.series-depth}") int seriesDepth) {
        this.restClient = restClient;
        this.kisAuthService = kisAuthService;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.seriesDepth = seriesDepth;
    }

    /**
     * 국내 지수 시계열 조회.
     *
     * @param code KOSPI 또는 KOSDAQ만 허용
     * @return seriesDepth 일치 IndexSeries. 실패 시 empty.
     * @throws IllegalArgumentException 해외 지수 코드를 넘겼을 때
     */
    public IndexSeries fetch(IndexCode code) {
        if (code.market() != IndexCode.Market.DOMESTIC) {
            throw new IllegalArgumentException("Domestic client received non-domestic code: " + code);
        }

        try {
            KisDomesticIndexResponse response = restClient.get()
                    .uri(uri -> uri
                            .path(PATH)
                            .queryParam("FID_PERIOD_DIV_CODE", "D")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                            .queryParam("FID_INPUT_ISCD", code.externalCode())
                            .queryParam("FID_INPUT_DATE_1", LocalDate.now().format(DATE_FMT))
                            .build())
                    .header("authorization", "Bearer " + kisAuthService.getAccessToken())
                    .header("appkey", appKey)
                    .header("content-type","application/json; charset=UTF-8")
                    .header("appsecret", appSecret)
                    .header("tr_id", TR_ID)
                    .header("custtype", "P")
                    .retrieve()
                    .body(KisDomesticIndexResponse.class);

            if (response == null) {
                log.warn("KIS domestic indices null response for {}", code);
                return IndexSeries.empty(code);
            }
            if (!"0".equals(response.rtCd())) {
                log.warn("KIS domestic indices error code={} msgCd={} msg={} for {}",
                        response.rtCd(), response.msgCd(), response.msg1(), code);
                return IndexSeries.empty(code);
            }
            if (response.output2() == null || response.output2().isEmpty()) {
                // KIS debugging trap: rt_cd "0"이지만 output2 비어옴
                log.warn("KIS domestic indices empty output2 for {} — wrong ISCD or off-hours?", code);
                return IndexSeries.empty(code);
            }

            List<IndexBar> bars = response.output2().stream()
                    .limit(seriesDepth)
                    .map(this::toBar)
                    .toList();

            log.debug("KIS domestic indices fetched code={} bars={}", code, bars.size());
            return new IndexSeries(code, bars.get(0), bars);

        } catch (RestClientException e) {
            // 네트워크/타임아웃/4xx/5xx — 다음 스케줄에서 재시도
            log.error("KIS domestic indices call failed for {}: {}", code, e.getMessage());
            return IndexSeries.empty(code);
        } catch (Exception e) {
            // 매핑/파싱 등 예상 못한 에러도 폴백 (스케줄러 죽지 않게)
            log.error("KIS domestic indices unexpected error for {}: {}", code, e.getMessage(), e);
            return IndexSeries.empty(code);
        }
    }

    private IndexBar toBar(KisDomesticIndexResponse.Output2 o) {
        // 국내: change/changeRate가 절대값일 가능성 → sign 우선 적용해서 안전하게
        double signedChange = KisParseUtils.applySign(KisParseUtils.parseDoubleSafe(o.change()), o.sign());
        double signedRate   = KisParseUtils.applySign(KisParseUtils.parseDoubleSafe(o.changeRate()), o.sign());

        return new IndexBar(
                LocalDate.parse(o.date(), DATE_FMT),
                KisParseUtils.parseDoubleSafe(o.openPrice()),
                KisParseUtils.parseDoubleSafe(o.highPrice()),
                KisParseUtils.parseDoubleSafe(o.lowPrice()),
                KisParseUtils.parseDoubleSafe(o.closePrice()),
                signedChange,
                signedRate,
                KisParseUtils.parseLongSafe(o.volume())
        );
    }
}
