package com.ysh.config.scheduling;

import com.ysh.domain.collection.entity.RunTrigger;
import com.ysh.domain.market.scheduler.MarketCollectionJobs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 부팅 직후 마켓 데이터 1회 수집. 각 작업은 실패해도 다음 작업으로 넘어간다(기록기가 예외 흡수).
 * scheduling.enabled=false (테스트)에선 비활성화.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class MarketBootstrap {

    private final MarketCollectionJobs jobs;

    @EventListener(ApplicationReadyEvent.class)
    public void warmupOnStartup() {
        log.info("Market bootstrap: collecting initial data...");
        jobs.forex(RunTrigger.BOOTSTRAP);
        jobs.indices(RunTrigger.BOOTSTRAP);   // 뉴스 지표 칩이 지수를 참조하므로 뉴스보다 먼저
        jobs.news(RunTrigger.BOOTSTRAP);
        jobs.commodities(RunTrigger.BOOTSTRAP);
        log.info("Market bootstrap completed.");
    }
}
