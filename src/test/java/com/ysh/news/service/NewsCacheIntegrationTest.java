package com.ysh.news.service;

import com.ysh.IntegrationTest;
import com.ysh.domain.news.collector.HackerNewsCollector;
import com.ysh.domain.news.collector.HankyungEconomyCollector;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import com.ysh.domain.news.repository.NewsFeedRepository;
import com.ysh.domain.news.service.NewsCacheService;
import com.ysh.domain.news.service.NewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@TestPropertySource(properties = "scheduling.enabled=false")
class NewsCacheIntegrationTest extends IntegrationTest {

    @Autowired
    NewsService newsService;
    @Autowired
    NewsCacheService cacheService;
    @Autowired
    NewsFeedRepository newsFeedRepository;

    @MockitoBean
    HackerNewsCollector hackerNewsCollector;
    @MockitoBean
    HankyungEconomyCollector hankyungCollector;

    @BeforeEach
    void setUp() {
        newsFeedRepository.deleteAll();
        cacheService.evict(NewsCategory.IT);
        cacheService.evict(NewsCategory.ECONOMY);
        cacheService.evict(NewsCategory.AI);
    }

    @Test
    void collectAndCache_DB저장_그리고_카테고리별_Redis캐시() {
        // given
        when(hackerNewsCollector.getCategory()).thenReturn(NewsCategory.IT);
        when(hackerNewsCollector.getSource()).thenReturn("HackerNews");
        when(hackerNewsCollector.collect()).thenReturn(List.of(
                buildFeed("HackerNews", NewsCategory.IT, "HN 글", "https://hn.example/1")
        ));

        when(hankyungCollector.getCategory()).thenReturn(NewsCategory.ECONOMY);
        when(hankyungCollector.getSource()).thenReturn("Hankyung");
        when(hankyungCollector.collect()).thenReturn(List.of(
                buildFeed("Hankyung", NewsCategory.ECONOMY, "환율 동향", "https://hk.example/1")
        ));

        // when
        List<NewsFeed> feeds = newsService.fetchAllFeeds();   // 트랜잭션 X
        newsService.saveAll(feeds);                           // 트랜잭션 O (DB save)
        newsService.refreshAllCaches();                       // 트랜잭션 X (Redis put)

        // then — DB
        assertThat(newsFeedRepository.count()).isEqualTo(2);
        // then — 카테고리별 Redis
        assertThat(cacheService.get(NewsCategory.IT)).isPresent();
        assertThat(cacheService.get(NewsCategory.IT).get()).hasSize(1);
        assertThat(cacheService.get(NewsCategory.ECONOMY)).isPresent();
        assertThat(cacheService.get(NewsCategory.ECONOMY).get()).hasSize(1);
    }

    @Test
    void getLatestByCategory_RedisHIT_DB조회_안함() {
        cacheService.put(NewsCategory.IT, List.of(
                buildFeed("HN", NewsCategory.IT, "캐시된 글", "https://cached.example/1")
        ));

        List<NewsFeed> result = newsService.getLatestByCategory(NewsCategory.IT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("캐시된 글");
    }

    @Test
    void getLatestByCategory_RedisMISS_DB폴백_그리고_캐시채움() {
        // DB에만 박힘
        when(hackerNewsCollector.getCategory()).thenReturn(NewsCategory.IT);
        when(hackerNewsCollector.getSource()).thenReturn("HackerNews");
        when(hackerNewsCollector.collect()).thenReturn(List.of(
                buildFeed("HN", NewsCategory.IT, "DB에만 있는 글", "https://db.example/1")
        ));
        when(hankyungCollector.collect()).thenReturn(List.of());
        List<NewsFeed> feeds = newsService.fetchAllFeeds();   // 트랜잭션 X
        newsService.saveAll(feeds);                           // 트랜잭션 O (DB save)
        newsService.refreshAllCaches();                       // 트랜잭션 X (Redis put)
        cacheService.evict(NewsCategory.IT);   // Redis만 비움

        List<NewsFeed> result = newsService.getLatestByCategory(NewsCategory.IT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("DB에만 있는 글");
        // 캐시 채워짐
        assertThat(cacheService.get(NewsCategory.IT)).isPresent();
    }

    @Test
    void getLatestByCategory_DB도Redis도없음_빈리스트() {
        List<NewsFeed> result = newsService.getLatestByCategory(NewsCategory.AI);
        assertThat(result).isEmpty();
    }

    private NewsFeed buildFeed(String source, NewsCategory category,
                               String title, String url) {
        return NewsFeed.builder()
                .source(source)
                .category(category)
                .title(title)
                .url(url)
                .summary(null)
                .publishedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Test
    void fetchAllFeeds_한_collector_실패해도_다른_collector는_진행() {
        // given — Hankyung은 정상, HackerNews는 예외
        when(hankyungCollector.collect()).thenReturn(List.of(
                buildFeed("Hankyung", NewsCategory.ECONOMY, "정상 글", "https://hk.example/1")
        ));
        when(hackerNewsCollector.collect())
                .thenThrow(new RuntimeException("HN API down"));

        // when
        List<NewsFeed> feeds = newsService.fetchAllFeeds();
        newsService.saveAll(feeds);

        // then
        assertThat(newsFeedRepository.count()).isEqualTo(1);
        assertThat(newsFeedRepository.findAll().get(0).getSource()).isEqualTo("Hankyung");
    }
}