package com.ysh.domain.market.client;

import com.ysh.domain.market.domain.ForexRate;
import com.ysh.domain.market.domain.ForexRates;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * dev, test 용
 */
@Component
@Profile("!prod & !local & !test")
public class MockForexClient implements ForexClient{
    @Override
    public ForexRates fetch() {
        return new ForexRates(LocalDateTime.now(), List.of(
                rate("USD",      "미국 달러",     1380.50),
                rate("EUR",      "유럽연합 유로",  1490.30),
                rate("JPY(100)", "일본 옌(100엔)", 920.15),
                rate("CNH",      "위안화",         190.20)
        ));
    }
    private ForexRate rate(String currency, String name, double rate) {
        double noise = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.01; // ±0.5%
        return new ForexRate(currency, name, rate);
    }
}
