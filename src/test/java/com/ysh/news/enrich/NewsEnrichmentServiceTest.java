package com.ysh.news.enrich;

import com.ysh.config.JacksonConfig;
import com.ysh.domain.news.enrich.NewsEnrichmentService;
import com.ysh.domain.news.enrich.NewsEnrichmentWriter;
import com.ysh.domain.news.enrich.NewsEnrichmentWriter.EnrichedNews;
import com.ysh.domain.news.enrich.NewsKeywordCodec;
import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import com.ysh.domain.news.repository.NewsFeedRepository;
import com.ysh.infra.llm.ClaudeClient;
import com.ysh.infra.llm.ClaudeNewsEnrichmentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * 뉴스 보강 단위 테스트 — LLM 응답을 신뢰하지 않는 부분(id 검증, 정규화, 실패 집계) 위주.
 */
class NewsEnrichmentServiceTest {

    private final NewsFeedRepository repository = mock(NewsFeedRepository.class);
    private final NewsEnrichmentWriter writer = mock(NewsEnrichmentWriter.class);
    private final ClaudeClient claudeClient = mock(ClaudeClient.class);
    private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

    private NewsEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new NewsEnrichmentService(
                repository, writer, new NewsKeywordCodec(objectMapper), claudeClient,
                new ClaudeNewsEnrichmentProperties("test-model", 1024, 0.0), objectMapper);
        when(writer.apply(any(), any())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
    }

    @Test
    void 정상응답_번역은_IT만_키워드는_정규화() {
        NewsFeed it = feed(1L, NewsCategory.IT, "Nvidia expands chip supply");
        NewsFeed eco = feed(2L, NewsCategory.ECONOMY, "반도체 수출 증가");
        givenPending(it, eco);
        when(claudeClient.complete(any())).thenReturn("""
                ```json
                {"items":[
                  {"id":1,"title_ko":"엔비디아, 반도체 공급 확대","keywords":["반도체"," #엔비디아 ","반도체","AI","클라우드"]},
                  {"id":2,"title_ko":"번역하면 안 됨","keywords":["반도체","수출"]}
                ]}
                ```""");

        NewsEnrichmentService.EnrichResult result = service.enrichPending();

        List<EnrichedNews> applied = capturedSucceeded();
        assertThat(result.enriched()).isEqualTo(2);
        assertThat(applied).extracting(EnrichedNews::id).containsExactlyInAnyOrder(1L, 2L);

        EnrichedNews itResult = applied.stream().filter(e -> e.id() == 1L).findFirst().orElseThrow();
        assertThat(itResult.titleKo()).isEqualTo("엔비디아, 반도체 공급 확대");
        assertThat(itResult.keywordsJson()).isEqualTo("[\"반도체\",\"엔비디아\",\"AI\"]");   // 중복·# 제거, 3개 상한

        EnrichedNews ecoResult = applied.stream().filter(e -> e.id() == 2L).findFirst().orElseThrow();
        assertThat(ecoResult.titleKo()).isNull();   // 경제 뉴스는 번역 저장 안 함
    }

    @Test
    void 배치에_없는_id와_누락된_id() {
        givenPending(feed(1L, NewsCategory.IT, "a"), feed(2L, NewsCategory.IT, "b"));
        when(claudeClient.complete(any())).thenReturn("""
                {"items":[{"id":1,"title_ko":"가","keywords":["AI"]},{"id":999,"title_ko":"x","keywords":[]}]}""");

        NewsEnrichmentService.EnrichResult result = service.enrichPending();

        assertThat(capturedSucceeded()).extracting(EnrichedNews::id).containsExactly(1L);
        assertThat(capturedFailed()).containsExactly(2L);   // 응답에서 빠진 건만 실패 처리
        assertThat(result.failed()).isEqualTo(1);
    }

    @Test
    void 빈응답은_인프라문제라_시도횟수를_올리지_않음() {
        givenPending(feed(1L, NewsCategory.IT, "a"));
        when(claudeClient.complete(any())).thenReturn("");

        NewsEnrichmentService.EnrichResult result = service.enrichPending();

        assertThat(capturedSucceeded()).isEmpty();
        assertThat(capturedFailed()).isEmpty();
        assertThat(result.failed()).isZero();
    }

    @Test
    void 파싱불가_응답은_배치전체_실패() {
        givenPending(feed(1L, NewsCategory.IT, "a"), feed(2L, NewsCategory.ECONOMY, "b"));
        when(claudeClient.complete(any())).thenReturn("죄송하지만 처리할 수 없습니다.");

        service.enrichPending();

        assertThat(capturedFailed()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void 대기건_없으면_LLM_호출_안함() {
        givenPending();

        service.enrichPending();

        verifyNoInteractions(claudeClient, writer);
    }

    // ───────────────── helpers ─────────────────

    private void givenPending(NewsFeed... feeds) {
        when(repository.findEnrichPending(anyInt(), any(), any(), any())).thenReturn(List.of(feeds));
    }

    private static NewsFeed feed(Long id, NewsCategory category, String title) {
        NewsFeed feed = NewsFeed.builder()
                .source(category == NewsCategory.IT ? "HackerNews" : "Hankyung")
                .category(category)
                .title(title)
                .url("https://example.com/" + id)
                .publishedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(feed, "id", id);
        return feed;
    }

    @SuppressWarnings("unchecked")
    private List<EnrichedNews> capturedSucceeded() {
        ArgumentCaptor<List<EnrichedNews>> captor = ArgumentCaptor.forClass(List.class);
        verify(writer).apply(captor.capture(), any());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Collection<Long> capturedFailed() {
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(writer).apply(any(), captor.capture());
        return captor.getValue();
    }
}
