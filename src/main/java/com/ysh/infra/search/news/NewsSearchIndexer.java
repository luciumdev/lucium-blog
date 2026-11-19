package com.ysh.infra.search.news;

import com.ysh.domain.news.enrich.NewsKeywordCodec;
import com.ysh.domain.news.entity.NewsFeed;
import com.ysh.domain.news.repository.NewsFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * news_feeds(보강 완료) → ES news 인덱스 동기화.
 *
 * <p>이벤트 기반 단건 색인 대신 "최근 N일 보강분 전체 재색인"을 매 수집 주기마다 수행.
 * id 가 같으면 덮어쓰기라 멱등이고, ES 가 잠깐 죽어 있었어도 다음 주기에 자동 복구된다.
 * 대상은 하루 수십 건 규모라 bulk 1회로 충분.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsSearchIndexer {

    static final int REINDEX_DAYS = 7;

    private final NewsFeedRepository newsFeedRepository;
    private final NewsSearchRepository newsSearchRepository;
    private final NewsKeywordCodec keywordCodec;

    @Transactional(readOnly = true)
    public int reindexRecent() {
        List<NewsFeed> feeds = newsFeedRepository.findEnrichedSince(
                LocalDateTime.now().minusDays(REINDEX_DAYS));
        if (feeds.isEmpty()) return 0;

        List<NewsSearchDocument> docs = feeds.stream().map(this::toDocument).toList();
        try {
            newsSearchRepository.saveAll(docs);
            log.info("뉴스 ES 재색인 완료. count={}", docs.size());
            return docs.size();
        } catch (Exception e) {
            // 다음 주기에 같은 범위를 다시 색인하므로 여기서는 기록만
            log.error("뉴스 ES 재색인 실패. count={}, cause={}", docs.size(), e.getMessage());
            return 0;
        }
    }

    private NewsSearchDocument toDocument(NewsFeed feed) {
        return new NewsSearchDocument(
                feed.getId(),
                feed.getCategory().name(),
                feed.getSource(),
                feed.getTitle(),
                feed.getTitleKo(),
                keywordCodec.fromJson(feed.getKeywords()),
                feed.getPublishedAt());
    }
}
