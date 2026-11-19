package com.ysh.domain.market.client;

import com.ysh.domain.market.client.dto.AlphaVantageCommodityResponse;
import com.ysh.domain.market.client.dto.GoldApiResponse;
import com.ysh.domain.market.domain.Commodities;
import com.ysh.domain.market.domain.CommodityCode;
import com.ysh.domain.market.domain.CommodityPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Profile({"prod", "local"})
public class CommoditiesRestApiClient implements CommoditiesClient{

    // Alpha Vantage function 식별자
    private static final String FUNCTION_WTI    = "WTI";
    private static final String FUNCTION_COPPER = "COPPER";

    // 통화/단위 (외부 API 표현 추상화 → 도메인 표현)
    private static final String CURRENCY_USD = "USD";
    private static final String UNIT_OZ      = "oz";
    private static final String UNIT_BBL     = "bbl";
    private static final String UNIT_LB      = "lb";

    // 정밀도 — BigDecimal scale=2 (소수점 둘째 자리)
    private static final int PRICE_SCALE = 2;

    private final RestClient goldApiClient;
    private final RestClient alphaVantageApiClient;
    private final String goldApiKey;
    private final String alphaVantageApiKey;

    public CommoditiesRestApiClient(
            @Qualifier("goldApiRestClient") RestClient goldApiClient,
            @Qualifier("alphaVantageApiRestClient") RestClient alphaVantageApiClient,
            @Value("${market.commodities.goldapi.api-key}") String goldApiKey,
            @Value("${market.commodities.alphavantage.api-key}") String alphaVantageApiKey) {
        this.goldApiClient = goldApiClient;
        this.alphaVantageApiClient = alphaVantageApiClient;
        this.goldApiKey = goldApiKey;
        this.alphaVantageApiKey = alphaVantageApiKey;
    }

    @Override
    public Commodities fetch() {
        List<CommodityPrice> prices = new ArrayList<>();

        if (goldApiKey != null && !goldApiKey.isBlank()) {
            try {
                prices.addAll(fetchGoldApi());
            } catch (Exception e) {
                log.warn("GoldAPI fetch failed: {}", shortReason(e));
            }
        } else {
            log.warn("GOLDAPI_KEY not set. Skipping gold/silver fetch.");
        }

        if (alphaVantageApiKey != null && !alphaVantageApiKey.isBlank()) {
            try {
                prices.addAll(fetchAlphaVantage());
            } catch (Exception e) {
                log.warn("Alpha Vantage fetch failed: {}", shortReason(e));
            }
        } else {
            log.warn("ALPHAVANTAGE_KEY not set. Skipping alphaVantage fetch.");
        }

        return new Commodities(LocalDateTime.now(), prices);
    }

    /**
     * GoldAPI 호출 — XAU(금)/XAG(은) 각각 호출, 통합 리턴.
     * 한쪽 실패해도 다른 쪽 진행 (부분 실패 허용).
     */
    private List<CommodityPrice> fetchGoldApi() {
        List<CommodityPrice> result = new ArrayList<>();

        try {
            GoldApiResponse xau = callGoldApi("XAU");
            result.add(toCommodityPrice(xau, CommodityCode.GOLD, "금"));
        } catch (Exception e) {
            log.warn("GoldAPI XAU(gold) fetch failed: {}", shortReason(e));
        }

        try {
            GoldApiResponse xag = callGoldApi("XAG");
            result.add(toCommodityPrice(xag, CommodityCode.SILVER, "은"));
        } catch (Exception e) {
            log.warn("GoldAPI XAG(silver) fetch failed: {}", shortReason(e));
        }

        return result;
    }

    private GoldApiResponse callGoldApi(String metal) {
        return goldApiClient.get()
                .uri("/api/{metal}/USD", metal)
                .header("x-access-token", goldApiKey)
                .retrieve()
                .body(GoldApiResponse.class);
    }

    private CommodityPrice toCommodityPrice(GoldApiResponse r, CommodityCode code, String name) {
        return new CommodityPrice(
                code,
                name,
                BigDecimal.valueOf(r.price()).setScale(PRICE_SCALE, RoundingMode.HALF_UP),
                BigDecimal.valueOf(r.ch()).setScale(PRICE_SCALE, RoundingMode.HALF_UP),
                BigDecimal.valueOf(r.chp()).setScale(PRICE_SCALE, RoundingMode.HALF_UP),
                r.currency() != null ? r.currency() : CURRENCY_USD,
                UNIT_OZ
        );
    }

    /**
     * Alpha Vantage 호출 — WTI/COPPER function endpoint 각각 호출, 시계열 응답에서 변동 계산.
     * 한쪽 실패해도 다른 쪽 진행 (부분 실패 허용).
     */
    private List<CommodityPrice> fetchAlphaVantage() {
        List<CommodityPrice> result = new ArrayList<>();

        try {
            AlphaVantageCommodityResponse wti = callAlphaVantage(FUNCTION_WTI);
            CommodityPrice price = toCommodityPrice(wti, CommodityCode.WTI, "WTI", UNIT_BBL);
            if (price != null) result.add(price);
        } catch (Exception e) {
            log.warn("Alpha Vantage WTI fetch failed: {}", shortReason(e));
        }

        try {
            AlphaVantageCommodityResponse copper = callAlphaVantage(FUNCTION_COPPER);
            CommodityPrice price = toCommodityPrice(copper, CommodityCode.COPPER, "구리", UNIT_LB);
            if (price != null) result.add(price);
        } catch (Exception e) {
            log.warn("Alpha Vantage COPPER fetch failed: {}", shortReason(e));
        }

        return result;
    }

    private AlphaVantageCommodityResponse callAlphaVantage(String function) {
        return alphaVantageApiClient.get()
                .uri(b -> b.path("/query")
                        .queryParam("function", function)
                        .queryParam("interval", "daily")
                        .queryParam("apikey", alphaVantageApiKey)
                        .build())
                .retrieve()
                .body(AlphaVantageCommodityResponse.class);
    }

    /**
     * Alpha Vantage 시계열 응답 → CommodityPrice 매핑.
     *
     * 변동 계산 — data[0](최신) - data[1](전일). data 부족하거나 value 파싱 실패 시 null 반환.
     * value가 {@code "."} 또는 {@code "N/A"}일 가능성 처리 (휴장일 데이터).
     */
    private CommodityPrice toCommodityPrice(AlphaVantageCommodityResponse r,
                                            CommodityCode code, String name, String unit) {
        if (r == null || r.data() == null || r.data().isEmpty()) {
            log.warn("Alpha Vantage {} response has no data", code);
            return null;
        }

        Double latest = parseValue(r.data().get(0).value());
        if (latest == null) {
            log.warn("Alpha Vantage {} latest value parse failed: {}", code, r.data().get(0).value());
            return null;
        }

        // 변동 계산 — 전일 데이터 있을 때만. 없으면 0으로 (음수 표시 회피)
        double changeValue = 0.0;
        double changePercent = 0.0;
        if (r.data().size() >= 2) {
            Double prev = parseValue(r.data().get(1).value());
            if (prev != null && prev > 0) {
                changeValue = latest - prev;
                changePercent = (changeValue / prev) * 100;
            }
        }

        return new CommodityPrice(
                code,
                name,
                BigDecimal.valueOf(latest).setScale(PRICE_SCALE, RoundingMode.HALF_UP),
                BigDecimal.valueOf(changeValue).setScale(PRICE_SCALE, RoundingMode.HALF_UP),
                BigDecimal.valueOf(changePercent).setScale(PRICE_SCALE, RoundingMode.HALF_UP),
                CURRENCY_USD,
                unit
        );
    }

    /**
     * Alpha Vantage value 파싱 — 휴장일은 {@code "."} 또는 {@code "N/A"}로 옴. 그 경우 null.
     */
    private static Double parseValue(String value) {
        if (value == null || value.isBlank() || ".".equals(value) || "N/A".equalsIgnoreCase(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String shortReason(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return e.getClass().getSimpleName();
        int colon = msg.indexOf(':');
        String head = colon > 0 ? msg.substring(0, colon) : msg;
        return head.length() > 100 ? head.substring(0, 100) : head;
    }
}
