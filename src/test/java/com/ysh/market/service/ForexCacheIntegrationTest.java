package com.ysh.market.service;

import com.ysh.IntegrationTest;
import com.ysh.common.cache.CacheResult;
import com.ysh.domain.market.client.ForexClient;
import com.ysh.domain.market.domain.ForexRate;
import com.ysh.domain.market.domain.ForexRates;
import com.ysh.domain.market.repository.MarketSnapshotRepository;
import com.ysh.domain.market.service.ForexCacheService;
import com.ysh.domain.market.service.ForexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = "scheduling.enabled=false")
public class ForexCacheIntegrationTest extends IntegrationTest {
    @Autowired
    ForexService forexService;
    @Autowired
    ForexCacheService cacheService;
    @Autowired
    MarketSnapshotRepository snapshotRepository;

    @MockitoBean   // Spring Boot 3.4+ 표준. @MockBean은 deprecated.
    ForexClient forexClient;

    @BeforeEach
    void setUp() {
        snapshotRepository.deleteAll();
        cacheService.evict();
    }

    @Test
    void collectAndSave_DB저장_그리고_Redis캐시() {
        ForexRates rates = sample("USD", "달러", 1380.0);
        when(forexClient.fetch()).thenReturn(rates);

        forexService.persistAndCache(rates);

        assertThat(snapshotRepository.count()).isEqualTo(1);
        assertThat(cacheService.getForexRates()).isPresent();
        assertThat(cacheService.getForexRates().get().rates().get(0).currencyCode()).isEqualTo("USD");
    }

    @Test
    void collectAndSave_빈응답_DB저장X_캐시X() {
        when(forexClient.fetch()).thenReturn(ForexRates.empty());

        ForexRates rates = forexService.fetchLatest();
        forexService.persistAndCache(rates);

        assertThat(snapshotRepository.count()).isZero();
        assertThat(cacheService.getForexRates()).isEmpty();
    }

    @Test
    void getLatest_RedisHIT_캐시값반환() {
        ForexRates cached = sample("EUR", "유로", 1500.0);
        cacheService.put(cached);

        CacheResult<ForexRates> result = forexService.getLatestCacheable();
        assertThat(result.isHit()).isTrue();
        assertThat(result.value().rates()).hasSize(1);
        assertThat(result.value().rates().get(0).currencyCode()).isEqualTo("EUR");
    }

    @Test
    void getLatest_RedisMISS_DB폴백_캐시채움() {
        // Given: DB에만 박혀있고 Redis는 비어있는 상태
        when(forexClient.fetch()).thenReturn(sample("JPY(100)", "엔(100엔)", 920.0));
        ForexRates rates = forexService.fetchLatest();
        forexService.persistAndCache(rates);
        cacheService.evict();           // Redis만 비움

        // When
        CacheResult<ForexRates> result = forexService.getLatestCacheable();


        // Then
        assertThat(result.isHit()).isFalse();   // MISS 시그널까지 검증
        assertThat(result.value().rates().get(0).currencyCode()).isEqualTo("JPY(100)");
        assertThat(cacheService.getForexRates()).isPresent();
    }

    @Test
    void getLatest_DB도Redis도없음_빈결과() {
        CacheResult<ForexRates> result = forexService.getLatestCacheable();
        assertThat(result.isHit()).isFalse();
        assertThat(result.value().isEmpty()).isTrue();
    }

    private ForexRates sample(String code, String name, double rate) {
        return new ForexRates(LocalDateTime.now(),
                List.of(new ForexRate(code, name, rate)));
    }
}
