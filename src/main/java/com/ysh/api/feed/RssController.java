package com.ysh.api.feed;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.entity.PostStatus;
import com.ysh.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * RSS 2.0 피드 — 최근 발행 글 20개.
 * 공개 주소: https://lucium.dev/api/v1/rss (nginx 가 /api 접두어를 떼고 백엔드로 전달)
 * ROME 은 뉴스 RSS 수집용으로 이미 의존성에 있어 추가 라이브러리 없음.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RssController {

    private static final int ITEM_COUNT = 20;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final MediaType RSS_XML = MediaType.parseMediaType("application/rss+xml;charset=UTF-8");

    private final PostRepository postRepository;

    @Value("${site.base-url:https://lucium.dev}")
    private String baseUrl;

    @GetMapping("/v1/rss")
    @Transactional(readOnly = true)
    public ResponseEntity<String> rss() throws FeedException {
        List<Post> posts = postRepository
                .findAllByStatus(PostStatus.PUBLISHED, PageRequest.of(0, ITEM_COUNT))
                .getContent();

        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setTitle("lucium.dev");
        feed.setLink(baseUrl);
        feed.setDescription("운영하며 배운 것을 기록하는 개발 블로그");
        feed.setLanguage("ko");
        feed.setEntries(posts.stream().map(this::toEntry).toList());
        if (!posts.isEmpty() && posts.get(0).getPublishedAt() != null) {
            feed.setPublishedDate(toDate(posts.get(0).getPublishedAt()));
        }

        return ResponseEntity.ok()
                .contentType(RSS_XML)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(new SyndFeedOutput().outputString(feed));
    }

    private SyndEntry toEntry(Post post) {
        String link = baseUrl + "/posts/" + post.getPublicId();
        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle(post.getTitle());
        entry.setLink(link);
        entry.setUri(link);
        if (post.getPublishedAt() != null) {
            entry.setPublishedDate(toDate(post.getPublishedAt()));
        }
        if (post.getSummary() != null && !post.getSummary().isBlank()) {
            SyndContent description = new SyndContentImpl();
            description.setType("text/plain");
            description.setValue(post.getSummary());
            entry.setDescription(description);
        }
        return entry;
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(KST).toInstant());
    }
}
