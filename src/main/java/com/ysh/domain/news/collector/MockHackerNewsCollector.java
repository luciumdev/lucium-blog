package com.ysh.domain.news.collector;

import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("!prod & !test")
public class MockHackerNewsCollector implements NewsCollector{
    private static final String SOURCE = "HackerNews";

    @Override public NewsCategory getCategory() { return NewsCategory.IT; }
    @Override public String getSource()         { return SOURCE; }

    @Override
    public List<NewsFeed> collect() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                build("Show HN: I built a CLI for managing PostgreSQL migrations",
                        "https://example.com/pg-cli", now.minusHours(2)),
                build("Why we moved from Kafka to NATS",
                        "https://example.com/kafka-nats", now.minusHours(5)),
                build("Rust 2.0 is here: what changed",
                        "https://example.com/rust-2", now.minusHours(8))
        );
    }

    private NewsFeed build(String title, String url, LocalDateTime publishedAt) {
        return NewsFeed.builder()
                .source(SOURCE)
                .category(NewsCategory.IT)
                .title(title)
                .url(url)
                .summary(null)
                .publishedAt(publishedAt)
                .build();
    }
}
