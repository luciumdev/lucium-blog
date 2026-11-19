package com.ysh.domain.news.collector;

import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("!prod & !test")
public class MockHankyungEconomyCollector implements NewsCollector {

    private static final String SOURCE = "Hankyung";


    @Override public NewsCategory getCategory() { return NewsCategory.ECONOMY; }
    @Override public String getSource()         { return SOURCE; }

    @Override
    public List<NewsFeed> collect() {
        LocalDateTime now = LocalDateTime.now();
        return List.of(
                build("한국은행, 기준금리 동결 결정",
                        "https://example.com/economy/rate-hold",
                        "금융통화위원회는 만장일치로 동결을 결정했다.",
                        now.minusHours(2)),
                build("코스피 사상 최고치 경신",
                        "https://example.com/economy/kospi-high",
                        "코스피지수가 2,800선을 돌파하며 강세를 보였다.",
                        now.minusHours(5))
        );
    }

    private NewsFeed build(String title, String url, String summary, LocalDateTime publishedAt) {
        return NewsFeed.builder()
                .source(SOURCE)
                .category(NewsCategory.ECONOMY)
                .title(title)
                .url(url)
                .summary(summary)
                .publishedAt(publishedAt)
                .build();
    }
}
