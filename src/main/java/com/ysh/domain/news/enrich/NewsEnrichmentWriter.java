package com.ysh.domain.news.enrich;

import com.ysh.domain.news.repository.NewsFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 보강 결과 DB 반영 — 배치 단위 짧은 트랜잭션.
 * LLM 호출(수 초)은 트랜잭션 밖에서 끝내고, 여기서는 UPDATE 만 한다.
 */
@Component
@RequiredArgsConstructor
public class NewsEnrichmentWriter {

    private final NewsFeedRepository newsFeedRepository;

    @Transactional
    public int apply(List<EnrichedNews> succeeded, Collection<Long> failedIds) {
        LocalDateTime now = LocalDateTime.now();
        int updated = 0;
        for (EnrichedNews e : succeeded) {
            updated += newsFeedRepository.markEnriched(e.id(), e.titleKo(), e.keywordsJson(), now);
        }
        if (!failedIds.isEmpty()) {
            newsFeedRepository.incrementEnrichAttempts(failedIds);
        }
        return updated;
    }

    public record EnrichedNews(Long id, String titleKo, String keywordsJson) {}
}
