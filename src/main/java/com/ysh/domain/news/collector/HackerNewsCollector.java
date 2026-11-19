package com.ysh.domain.news.collector;

import com.ysh.domain.news.dto.HackerNewsItem;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;


@Slf4j
@Component
@Profile({"prod", "test"})
public class HackerNewsCollector implements NewsCollector{

    private static final String SOURCE = "HackerNews";
    /** published_at 은 KST 로컬 시각으로 통일 (한경 RSS·지표 날짜 매칭 기준과 동일) */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final RestClient restClient;
    private final int topLimit;
    private final int scoreThreshold;
    private final Duration freshnessWindow;

    public HackerNewsCollector(@Qualifier("hackerNewsRestClient") RestClient restClient,
                               @Value("${news.hackernews.top-limit}") int topLimit,
                               @Value("${news.hackernews.score-threshold}") int scoreThreshold,
                               @Value("${news.hackernews.freshness-window}") Duration freshnessWindow) {
        this.restClient = restClient;
        this.topLimit = topLimit;
        this.scoreThreshold = scoreThreshold;
        this.freshnessWindow = freshnessWindow;
    }


    @Override
    public NewsCategory getCategory() {
        return NewsCategory.IT;
    }

    @Override
    public String getSource() {
        return SOURCE;
    }

    @Override
    public List<NewsFeed> collect() {
        try {
            List<Long> topIds = restClient.get()
                    .uri("/v0/topstories.json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Long>>() {});
            if (topIds == null || topIds.isEmpty()) {
                log.warn("HackerNews API returned empty body.");
                return List.of();
            }
            // cutoff은 collect() 호출당 한 번 계산해 lambda에 캡처 (Instant.now 매 item 호출 회피).
            Instant cutoff = Instant.now().minus(freshnessWindow);
            return topIds.stream()
                    .limit(topLimit)
                    .map(this::fetchItem)
                    .filter(Objects::nonNull)
                    .filter(this::isValidStory)
                    .filter(item -> isFresh(item, cutoff))
                    .map(this::toNewsFeed)
                    .toList();
        } catch (RestClientException e ) {
            log.error("HackerNews API request failed: {}", e.getMessage());
            return List.of();
        }
    }



    private HackerNewsItem fetchItem(Long id) {
        try {
            return restClient.get()
                    .uri("/v0/item/{id}.json", id)
                    .retrieve()
                    .body(HackerNewsItem.class);
        } catch (RestClientException e) {
            // 개별 아이템 실패는 전체 수집 중단 X — 나머지 진행
            log.warn("HackerNews item fetch failed. id={}, cause={}", id, e.getMessage());
            return null;
        }
    }

    private boolean isValidStory(HackerNewsItem item) {
        return "story".equals(item.type())
                && !Boolean.TRUE.equals(item.dead())
                && !Boolean.TRUE.equals(item.deleted())
                && item.url() != null && !item.url().isBlank()
                && item.score() != null && item.score() >= scoreThreshold
                && item.title() != null && !item.title().isBlank()
                && item.time() != null;
    }

    /**
     * 신선도 필터: HN item의 time(epoch seconds)이 cutoff 이후인지 검증.
     *{@link #isValidStory(HackerNewsItem)}에서 {@code time != null} 검증된 후에만 호출
     */
    private boolean isFresh(HackerNewsItem item, Instant cutoff) {
        Instant published = Instant.ofEpochSecond(item.time());
        return !published.isBefore(cutoff);
    }

    private NewsFeed toNewsFeed(HackerNewsItem item) {
        return NewsFeed.builder()
                .source(SOURCE)
                .category(NewsCategory.IT)
                .title(truncate(item.title(), 500))
                .url(truncate(item.url(), 1000))
                .summary(null)
                // 기존: ZoneOffset.UTC 로 저장 → 화면에서 9시간 어긋남. KST 로 통일.
                .publishedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(item.time()), KST))
                .build();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }
}
