package com.ysh.domain.news.enrich;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import com.ysh.domain.news.enrich.NewsEnrichmentWriter.EnrichedNews;
import com.ysh.domain.news.repository.NewsFeedRepository;
import com.ysh.infra.llm.ClaudeClient;
import com.ysh.infra.llm.ClaudeNewsEnrichmentProperties;
import com.ysh.infra.llm.ClaudePromptRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 뉴스 보강 — 수집 직후 LLM 배치로 (1) HN 제목 번역 (2) 키워드 정규화.
 *
 * <p>설계:
 * <ul>
 *   <li>멱등: enriched_at IS NULL 인 행만 대상. 반영 UPDATE 도 같은 조건.</li>
 *   <li>실패 격리: 배치 단위로 LLM 호출. 한 배치 실패가 다른 배치·수집 파이프라인을 막지 않음.</li>
 *   <li>재시도 상한: 실패 시 enrich_attempts +1, {@value #MAX_ATTEMPTS}회 이상이면 제외.</li>
 *   <li>LLM 불신: 응답 id 는 배치에 있던 id 만 인정, 키워드는 저장 전 정규화.</li>
 *   <li>트랜잭션 분리: LLM 호출은 트랜잭션 밖, UPDATE 만 {@link NewsEnrichmentWriter} 에서.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsEnrichmentService {

    static final int MAX_ATTEMPTS = 3;
    private static final int BATCH_SIZE = 15;
    private static final int MAX_PER_RUN = 45;          // 1회 실행 최대 3배치 — 비용 상한
    private static final int LOOKBACK_DAYS = 3;
    private static final int MAX_TITLE_KO_LENGTH = 500;
    private static final List<NewsCategory> TARGETS = List.of(NewsCategory.IT, NewsCategory.ECONOMY);

    private final NewsFeedRepository newsFeedRepository;
    private final NewsEnrichmentWriter writer;
    private final NewsKeywordCodec keywordCodec;
    private final ClaudeClient claudeClient;
    private final ClaudeNewsEnrichmentProperties props;
    private final ObjectMapper objectMapper;

    public EnrichResult enrichPending() {
        List<NewsFeed> pending = newsFeedRepository.findEnrichPending(
                MAX_ATTEMPTS, TARGETS,
                LocalDateTime.now().minusDays(LOOKBACK_DAYS),
                PageRequest.of(0, MAX_PER_RUN));
        if (pending.isEmpty()) {
            return EnrichResult.EMPTY;
        }

        int enriched = 0;
        int failed = 0;
        for (int from = 0; from < pending.size(); from += BATCH_SIZE) {
            List<NewsFeed> batch = pending.subList(from, Math.min(from + BATCH_SIZE, pending.size()));
            BatchOutcome outcome = processBatch(batch);
            try {
                enriched += writer.apply(outcome.succeeded(), outcome.failedIds());
                failed += outcome.failedIds().size();
            } catch (Exception e) {
                // DB 반영 실패 — 다음 실행에서 그대로 다시 대기 건으로 잡힌다 (attempts 증가 없음)
                log.error("뉴스 보강 반영 실패. batchSize={}", batch.size(), e);
            }
        }
        log.info("뉴스 보강 완료. pending={}, enriched={}, failed={}", pending.size(), enriched, failed);
        return new EnrichResult(pending.size(), enriched, failed);
    }

    // ───────────────────────── private ─────────────────────────

    private BatchOutcome processBatch(List<NewsFeed> batch) {
        Map<Long, NewsFeed> byId = batch.stream()
                .collect(Collectors.toMap(NewsFeed::getId, Function.identity()));

        String text;
        try {
            text = claudeClient.complete(new ClaudePromptRequest(
                    NewsEnrichmentPrompt.SYSTEM,
                    NewsEnrichmentPrompt.user(batch),
                    props.maxTokens(),
                    props.temperature(),
                    props.model()));
        } catch (Exception e) {
            log.warn("뉴스 보강 LLM 호출 실패: {}", e.getMessage());
            return BatchOutcome.skipped();
        }
        if (text == null || text.isBlank()) {
            // 빈 응답 = API 키 없음·네트워크 등 인프라 문제. 기사 탓이 아니므로 attempts 를 올리지 않는다.
            log.warn("뉴스 보강 LLM 응답 없음 — 다음 실행에서 재시도");
            return BatchOutcome.skipped();
        }

        List<EnrichmentLlmResponse.Item> items = parse(text);
        List<EnrichedNews> succeeded = new ArrayList<>();
        Set<Long> done = new HashSet<>();

        for (EnrichmentLlmResponse.Item item : items) {
            if (item.id() == null || !byId.containsKey(item.id()) || !done.add(item.id())) {
                continue;   // 배치에 없던 id·중복 id 무시
            }
            NewsFeed feed = byId.get(item.id());
            succeeded.add(new EnrichedNews(
                    feed.getId(),
                    sanitizeTitleKo(feed, item.titleKo()),
                    keywordCodec.toJson(item.keywords())));
        }

        Set<Long> failedIds = new HashSet<>(byId.keySet());
        failedIds.removeAll(done);
        return new BatchOutcome(succeeded, failedIds);
    }

    private List<EnrichmentLlmResponse.Item> parse(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            log.warn("뉴스 보강 응답에서 JSON을 찾지 못함");
            return List.of();
        }
        try {
            return objectMapper.readValue(text.substring(start, end + 1), EnrichmentLlmResponse.class).items();
        } catch (Exception e) {
            log.warn("뉴스 보강 응답 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /** 번역은 IT(영어 원문)만 저장. 원문과 같거나 비어 있으면 null. */
    private String sanitizeTitleKo(NewsFeed feed, String titleKo) {
        if (feed.getCategory() != NewsCategory.IT || titleKo == null) return null;
        String t = titleKo.strip();
        if (t.isEmpty() || "null".equalsIgnoreCase(t) || t.equals(feed.getTitle())) return null;
        return t.length() <= MAX_TITLE_KO_LENGTH ? t : t.substring(0, MAX_TITLE_KO_LENGTH);
    }

    private record BatchOutcome(List<EnrichedNews> succeeded, Set<Long> failedIds) {
        /** 반영할 것도, 실패로 셀 것도 없음 */
        static BatchOutcome skipped() {
            return new BatchOutcome(List.of(), Set.of());
        }
    }

    public record EnrichResult(int pending, int enriched, int failed) {
        static final EnrichResult EMPTY = new EnrichResult(0, 0, 0);
    }
}
