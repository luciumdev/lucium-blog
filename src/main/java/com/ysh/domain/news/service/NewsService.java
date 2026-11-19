package com.ysh.domain.news.service;

import com.ysh.common.cache.CacheResult;
import com.ysh.domain.news.collector.NewsCollector;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import com.ysh.domain.news.repository.NewsFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final List<NewsCollector> collectors;
    private final NewsFeedRepository newsFeedRepository;
    private final NewsCacheService cacheService;

    @Value("${news.cache-top-n}")
    private int cacheTopN;

    /**
     * 외부 RSS 수집만. 트랜잭션 밖. collector 단위 부분 실패 허용 — 한 collector가
     * 예외 던져도 다른 collector는 그대로 진행.
     */
    public List<NewsFeed> fetchAllFeeds() {
        List<NewsFeed> all = new ArrayList<>();
        for (NewsCollector collector : collectors) {
            try {
                List<NewsFeed> feeds = collector.collect();
                all.addAll(feeds);
                log.info("News fetched from {}: {} feeds",
                        collector.getClass().getSimpleName(), feeds.size());
            } catch (Exception e) {
                log.warn("Collector {} failed — skip and continue",
                        collector.getClass().getSimpleName(), e);
            }
        }
        return all;
    }

    /**
     * DB insert만. 트랜잭션. insertIfNotExists로 dedup.
     */
    @Transactional
    public CollectResult saveAll(List<NewsFeed> feeds) {
        int inserted = 0;
        for (NewsFeed feed : feeds) {
            int rows = newsFeedRepository.insertIfNotExists(
                    feed.getSource(),
                    feed.getCategory().name(),
                    feed.getTitle(),
                    feed.getUrl(),
                    feed.getSummary(),
                    feed.getPublishedAt(),
                    feed.getFetchedAt()
            );
            if (rows > 0) inserted++;
        }
        log.info("News persisted. attempted={}, inserted={}", feeds.size(), inserted);
        return new CollectResult(feeds.size(), inserted);
    }


    public void refreshAllCaches() {
        for (NewsCategory category : NewsCategory.values()) {
            try {
                List<NewsFeed> feeds = newsFeedRepository.findLatestByCategory(
                        category, PageRequest.of(0, cacheTopN));
                cacheService.put(category, feeds);
            } catch (Exception e) {
                log.warn("News cache refresh failed. category={}", category, e);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NewsFeed> getLatestByCategory(NewsCategory category) {
        return cacheService.get(category)
                .orElseGet(() -> loadFromDbAndCache(category));
    }

    @Transactional(readOnly = true)
    public CacheResult<List<NewsFeed>> getLatestByCategoryCacheable(NewsCategory category) {
        return cacheService.get(category)
                .map(CacheResult::hit)
                .orElseGet(() -> CacheResult.miss(loadFromDbAndCache(category)));
    }

    private List<NewsFeed> loadFromDbAndCache(NewsCategory category) {
        List<NewsFeed> fromDb = newsFeedRepository.findLatestByCategory(
                category, PageRequest.of(0, cacheTopN));
        if (!fromDb.isEmpty()) {
            cacheService.put(category, fromDb);
        }
        return fromDb;
    }

    public record CollectResult(int attempted, int inserted) {}
}
