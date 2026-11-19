package com.ysh.domain.market.client;

import com.ysh.domain.market.client.dto.KoreaEximResponse;
import com.ysh.domain.market.domain.ForexRate;
import com.ysh.domain.market.domain.ForexRates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@Profile({"prod", "local", "test"})
public class KoreaEximForexClient implements ForexClient{

    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final List<String> TARGET_CURRENCIES  = List.of("USD", "EUR", "JPY(100)", "CNH");

    private final RestClient restClient;
    private final String apiKey;

    public KoreaEximForexClient(@Qualifier("forexRestClient") RestClient restClient,
                                @Value("${market.forex.api-key}") String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    public ForexRates fetch() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("KOREA_FOREX_API_KEY is not set. Returning empty.");
            return ForexRates.empty();
        }

        try {
            List<KoreaEximResponse> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/site/program/financial/exchangeJSON")
                            .queryParam("authkey", apiKey)
                            .queryParam("searchdate", LocalDate.now().format(DATE_FMT))
                            .queryParam("data", "AP01")
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<KoreaEximResponse>>() {});

            if (body == null || body.isEmpty()) {
                // 주말/공휴일/영업시간 외(11시 이전)에 빈 배열 반환됨
                log.info("Forex API returned empty body. weekend/holiday or before 11:00?");
                return ForexRates.empty();
            }

            // result 코드 검증 — 첫 element만 봐도 충분 (전체 응답이 같은 result)
            int result = body.get(0).result();
            if (result != 1) {
                log.warn("Forex API non-success result={} (1=OK, 2=data, 3=auth, 4=limit)", result);
                return ForexRates.empty();
            }

            List<ForexRate> rates = body.stream()
                    .filter(r -> TARGET_CURRENCIES.contains(r.curUnit()))
                    .map(this::toForexRate)
                    .toList();

            return new ForexRates(LocalDateTime.now(), rates);

        } catch (RestClientException e) {
            // 네트워크/타임아웃/4xx/5xx — 다음 스케줄에서 재시도. 마지막 정상 스냅샷이 DB에 살아있음.
            log.error("Forex API call failed. cause={}", e.getMessage());
            return ForexRates.empty();
        }
    }

    private ForexRate toForexRate(KoreaEximResponse r) {
        // "1,234.56" → 1234.56
        double dealBasR = Double.parseDouble(r.dealBasR().replace(",", ""));
        return new ForexRate(r.curUnit(), r.curNm(), dealBasR);
    }
}
