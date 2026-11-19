package com.ysh.domain.news.service;

import com.ysh.domain.collection.entity.RunOutcome;
import com.ysh.domain.news.enrich.NewsEnrichmentService;
import com.ysh.domain.news.entity.NewsFeed;
import com.ysh.infra.search.news.NewsSearchIndexer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 뉴스 수집 파이프라인 — 스케줄러와 부팅 워밍업이 같은 순서를 쓰도록 한 곳에 모음.
 *
 * <pre>
 * 수집(RSS/HN, 트랜잭션 밖) → 저장(멱등 INSERT) → 보강(LLM 번역·키워드) → ES 재색인 → Redis 캐시 갱신
 * </pre>
 * 단계별 실패 격리: 앞 단계가 실패해도 뒤 단계는 실행한다.
 * 예) LLM 장애 → 보강만 건너뛰고 새 기사는 원문 그대로 캐시에 반영됨 → 결과는 PARTIAL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsPipeline {

    private final NewsService newsService;
    private final NewsEnrichmentService enrichmentService;
    private final NewsSearchIndexer searchIndexer;

    /**
     * @return SUCCESS(새 기사 수) / PARTIAL(일부 단계 실패) / FAILED(수집 단계 실패 또는 수집 0건)
     */
    public RunOutcome run() {
        List<String> failedStages = new ArrayList<>();

        NewsService.CollectResult collected = stage("collect", failedStages, () -> {
            List<NewsFeed> feeds = newsService.fetchAllFeeds();
            return newsService.saveAll(feeds);
        });
        NewsEnrichmentService.EnrichResult enriched = stage("enrich", failedStages, enrichmentService::enrichPending);
        stage("index", failedStages, searchIndexer::reindexRecent);
        stage("cache", failedStages, () -> {
            newsService.refreshAllCaches();
            return Boolean.TRUE;
        });

        if (collected == null) {
            return RunOutcome.failed("수집 단계 실패");
        }
        if (collected.attempted() == 0) {
            // 모든 수집기가 빈 결과 — 외부 API 장애 가능성이 높아 실패로 본다
            return RunOutcome.failed("수집 0건 (모든 소스 응답 없음)");
        }
        if (enriched != null && enriched.failed() > 0) {
            failedStages.add("enrich(" + enriched.failed() + "건)");
        }
        return failedStages.isEmpty()
                ? RunOutcome.success(collected.inserted())
                : RunOutcome.partial(collected.inserted(), "실패 단계: " + String.join(", ", failedStages));
    }

    private <T> T stage(String name, List<String> failedStages, Supplier<T> task) {
        try {
            return task.get();
        } catch (Exception e) {
            log.error("News pipeline stage failed — continue. stage={}", name, e);
            failedStages.add(name);
            return null;
        }
    }
}
