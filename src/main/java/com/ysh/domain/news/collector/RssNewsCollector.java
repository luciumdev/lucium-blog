package com.ysh.domain.news.collector;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.ysh.domain.news.entity.NewsFeed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class RssNewsCollector implements NewsCollector {

    /** published_at 은 KST 로컬 시각으로 통일 — JVM 기본 시간대(TZ 환경변수)에 의존하지 않음 */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    protected final RestClient restClient;
    protected final String feedPath;

    protected RssNewsCollector(RestClient restClient, String feedPath) {
        this.restClient = restClient;
        this.feedPath = feedPath;
    }

    @Override
    public final List<NewsFeed> collect() {
        try {
            String xml = restClient.get()
                    .uri(feedPath)
                    .retrieve()
                    .body(String.class);
            if (xml == null || xml.isBlank()) {
                log.warn("RSS feed is empty.");
                return List.of();
            }
            SyndFeed feed = new SyndFeedInput().build(new StringReader(xml));

            return feed.getEntries().stream()
                    .filter(this::isValid)
                    .map(this::toNewsFeed)
                    .toList();
        } catch (Exception e){
            log.error("RSS feed fetch failed: {}", e.getMessage());
            return List.of();
        }
    }

    private boolean isValid(SyndEntry  entry) {
        return entry.getTitle() != null && !entry.getTitle().isBlank()
                && entry.getLink() != null && !entry.getLink().isBlank();
    }

    private NewsFeed toNewsFeed (SyndEntry  entry) {
        return NewsFeed.builder()
                .source(getSource())
                .category(getCategory())
                .title(truncate(entry.getTitle(), 500))
                .url(truncate(entry.getLink(),1000))
                .summary(extractSummary(entry))
                .publishedAt(toLocalDateTime(entry.getPublishedDate()))
                .build();
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) return LocalDateTime.now(KST);
        return date.toInstant().atZone(KST).toLocalDateTime();
    }

    private String extractSummary(SyndEntry entry) {
        SyndContent description = entry.getDescription();
        if (description == null) return null;
        String value = description.getValue();
        if (value == null) return null;

        String stripped = value.replaceAll("<[^>]+>", "").trim();
        return truncate(stripped, 1000);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
