package com.ysh.domain.market.client.kis;

import com.ysh.domain.market.client.dto.KisOverseasIndexResponse;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * KIS 해외 ETF 일별 시계열 클라이언트 (NASDAQ 프록시 QQQ, S&P 500 프록시 SPY).
 *
 * Endpoint: {@code GET /uapi/overseas-price/v1/quotations/dailyprice}
 * TR_ID: {@code HHDFS76240000}
 *
 * 응답 output2는 최신순 100건 시계열. seriesDepth만큼 take.
 *
 * 주의 — KIS는 종목-거래소(EXCD) mismatch 시 silent fail (rt_cd "0" + 빈 output2).
 * EXCD는 IndexCode 정의에 박혀있음 (QQQ→NAS, SPY→AMS).
 *
 * 실패 정책 — 4xx/5xx, 네트워크 에러, rt_cd 비정상, 빈 output2 모두 {@link IndexSeries#empty(IndexCode)}로 폴백.
 */
@Slf4j
@Component
public class KisOverseasIndicesClient {

    private static final String PATH = "/uapi/overseas-price/v1/quotations/dailyprice";
    private static final String TR_ID = "HHDFS76240000";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient restClient;
    private final KisAuthService kisAuthService;
    private final String appKey;
    private final String appSecret;
    private final int seriesDepth;

    public KisOverseasIndicesClient(
            @Qualifier("kisRestClient") RestClient restClient,
            KisAuthService kisAuthService,
            @Value("${market.kis.app-key}") String appKey,
            @Value("${market.kis.app-secret}") String appSecret,
            @Value("${market.kis.indices.series-depth}") int seriesDepth
    ) {
        this.restClient = restClient;
        this.kisAuthService = kisAuthService;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.seriesDepth = seriesDepth;
    }

    /**
     * 해외 지수 ETF 시계열 조회.
     *
     * @param code NASDAQ_PROXY 또는 SP500_PROXY만 허용
     * @return seriesDepth 일치 IndexSeries. 실패 시 empty.
     * @throws IllegalArgumentException 국내 지수 코드를 넘겼을 때
     */
    public IndexSeries fetch(IndexCode code) {
        if (code.market() != IndexCode.Market.OVERSEAS) {
            throw new IllegalArgumentException("Overseas client received non-overseas code: " + code);
        }

        try {
            KisOverseasIndexResponse response = restClient.get()
                    .uri(uri -> uri
                            .path(PATH)
                            // KIS 명세상 빈값 query param 필요 — 빈 문자열로 명시 박음
                            .queryParam("AUTH", "")
                            .queryParam("EXCD", code.exchangeCode())
                            .queryParam("SYMB", code.externalCode())
                            .queryParam("GUBN", "0")
                            .queryParam("BYMD", "")
                            .queryParam("MODP", "1")
                            .build())
                    .header("authorization", "Bearer " + kisAuthService.getAccessToken())
                    .header("appkey", appKey)
                    .header("appsecret", appSecret)
                    .header("tr_id", TR_ID)
                    .header("custtype", "P")
                    .retrieve()
                    .body(KisOverseasIndexResponse.class);

            if (response == null) {
                log.warn("KIS overseas indices null response for {}", code);
                return IndexSeries.empty(code);
            }
            if (!"0".equals(response.rtCd())) {
                log.warn("KIS overseas indices error code={} msgCd={} msg={} for {}",
                        response.rtCd(), response.msgCd(), response.msg1(), code);
                return IndexSeries.empty(code);
            }
            if (response.output2() == null || response.output2().isEmpty()) {
                // KIS debugging trap: EXCD-SYMB mismatch 또는 종목 미상장 등에서 발생
                log.warn("KIS overseas indices empty output2 for {} — EXCD={} mismatch?",
                        code, code.exchangeCode());
                return IndexSeries.empty(code);
            }

            List<IndexBar> bars = response.output2().stream()
                    .limit(seriesDepth)
                    .map(this::toBar)
                    .toList();

            log.debug("KIS overseas indices fetched code={} bars={}", code, bars.size());
            return new IndexSeries(code, bars.get(0), bars);

        } catch (RestClientException e) {
            log.error("KIS overseas indices call failed for {}: {}", code, e.getMessage());
            return IndexSeries.empty(code);
        } catch (Exception e) {
            log.error("KIS overseas indices unexpected error for {}: {}", code, e.getMessage(), e);
            return IndexSeries.empty(code);
        }
    }

    private IndexBar toBar(KisOverseasIndexResponse.Output2 o) {
        // 해외: changeRate는 부호 prefix 포함("+1.17")이지만 sign과 일관성 보장 안 됨 → sign 우선 적용
        // change는 prefix 유무 모호 → sign 적용
        double signedChange = KisParseUtils.applySign(KisParseUtils.parseDoubleSafe(o.change()), o.sign());
        double signedRate = KisParseUtils.applySign(KisParseUtils.parseDoubleSafe(o.changeRate()), o.sign());

        return new IndexBar(
                LocalDate.parse(o.date(), DATE_FMT),
                KisParseUtils.parseDoubleSafe(o.openPrice()),
                KisParseUtils.parseDoubleSafe(o.highPrice()),
                KisParseUtils.parseDoubleSafe(o.lowPrice()),
                KisParseUtils.parseDoubleSafe(o.close()),
                signedChange,
                signedRate,
                KisParseUtils.parseLongSafe(o.volume())
        );
    }
}