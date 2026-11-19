package com.ysh.domain.market.scheduler;

import com.ysh.domain.collection.entity.CollectionJob;
import com.ysh.domain.collection.entity.RunOutcome;
import com.ysh.domain.collection.entity.RunTrigger;
import com.ysh.domain.collection.service.CollectionRunRecorder;
import com.ysh.domain.market.domain.Commodities;
import com.ysh.domain.market.domain.ForexRates;
import com.ysh.domain.market.domain.IndexSeries;
import com.ysh.domain.market.domain.IndicesData;
import com.ysh.domain.market.service.CommoditiesService;
import com.ysh.domain.market.service.ForexService;
import com.ysh.domain.market.service.IndicesService;
import com.ysh.domain.news.service.NewsPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 마켓·뉴스 수집 작업 본문 — 스케줄러(정기)와 부트스트랩(기동 직후)이 공통으로 호출.
 * 각 작업은 결과를 직접 판정(RunOutcome)하고, 기록·예외 처리는 {@link CollectionRunRecorder} 가 맡는다.
 */
@Component
@RequiredArgsConstructor
public class MarketCollectionJobs {

    private final CollectionRunRecorder recorder;
    private final ForexService forexService;
    private final IndicesService indicesService;
    private final CommoditiesService commoditiesService;
    private final NewsPipeline newsPipeline;

    public RunOutcome forex(RunTrigger trigger) {
        return recorder.run(CollectionJob.FOREX, trigger, () -> {
            ForexRates rates = forexService.fetchLatest();
            if (rates.isEmpty()) {
                // 수출입은행 API 는 주말·공휴일에 빈 배열을 준다 → 오류가 아니라 EMPTY
                return RunOutcome.empty("응답 데이터 없음 (주말·공휴일 또는 API 오류)");
            }
            forexService.persistAndCache(rates);
            return RunOutcome.success(rates.rates().size());
        });
    }

    public RunOutcome indices(RunTrigger trigger) {
        return recorder.run(CollectionJob.INDICES, trigger, () -> {
            IndicesData data = indicesService.fetchAll();   // 외부 API (~5초, throttle)
            indicesService.persistAndCache(data);           // 트랜잭션
            List<IndexSeries> series = data.series();
            List<String> missing = series.stream()
                    .filter(IndexSeries::isEmpty)
                    .map(s -> s.code().name())
                    .toList();
            int ok = series.size() - missing.size();
            if (ok == 0) return RunOutcome.failed("지수 응답 없음");
            return missing.isEmpty()
                    ? RunOutcome.success(ok)
                    : RunOutcome.partial(ok, "응답 없음: " + String.join(", ", missing));
        });
    }

    public RunOutcome commodities(RunTrigger trigger) {
        return recorder.run(CollectionJob.COMMODITIES, trigger, () -> {
            Commodities commodities = commoditiesService.fetchLatest();
            commoditiesService.persistAndCache(commodities);
            int count = commodities.prices().size();
            return count == 0 ? RunOutcome.empty("응답 데이터 없음") : RunOutcome.success(count);
        });
    }

    public RunOutcome news(RunTrigger trigger) {
        return recorder.run(CollectionJob.NEWS, trigger, newsPipeline::run);
    }
}
