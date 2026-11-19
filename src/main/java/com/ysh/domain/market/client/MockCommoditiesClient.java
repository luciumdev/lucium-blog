package com.ysh.domain.market.client;

import com.ysh.domain.market.domain.Commodities;
import com.ysh.domain.market.domain.CommodityCode;
import com.ysh.domain.market.domain.CommodityPrice;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * test/dev 용 더미 원자재 클라이언트.
 *
 * {@code @Profile("!prod & !local")} — MockForexClient/MockClaudeClient와 동일 정책.
 * prod/local에선 비활성, 그 외 환경에서 활성. 노이즈 ±0.5% 적용으로 캐시 변경 감지 등 테스트.
 */
@Component
@Profile("!prod & !local")
public class MockCommoditiesClient implements CommoditiesClient {

    @Override
    public Commodities fetch() {
        return new Commodities(LocalDateTime.now(), List.of(
                price(CommodityCode.GOLD,   "금",   2318.50,  12.30,  0.53, "USD", "oz"),
                price(CommodityCode.SILVER, "은",     27.45,   0.18,  0.66, "USD", "oz"),
                price(CommodityCode.WTI,    "WTI",    78.20,  -0.55, -0.70, "USD", "bbl"),
                price(CommodityCode.COPPER, "구리",    4.42,   0.03,  0.68, "USD", "lb")
        ));
    }

    private CommodityPrice price(CommodityCode code, String name, double base,
                                 double change, double changeRate,
                                 String currency, String unit) {
        double noise = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.01;
        BigDecimal priceWithNoise = BigDecimal.valueOf(base * (1 + noise))
                .setScale(2, RoundingMode.HALF_UP);
        return new CommodityPrice(
                code, name, priceWithNoise,
                BigDecimal.valueOf(change).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(changeRate).setScale(2, RoundingMode.HALF_UP),
                currency, unit
        );
    }
}