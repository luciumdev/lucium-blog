package com.ysh.infra.aijournal;

import com.ysh.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.domain.aijournal.service.AiJournalArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/v1/ai-journal")
@RequiredArgsConstructor
public class AiJournalController {

    private static final String CACHE_KEY_BRIEFING = "ai:journal:briefing";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AiJournalArchiveService archiveService;

    /**
     * 오늘의 LLM 브리핑 + 전체 뉴스 목록 반환
     * 소스별 필터: ?source=OpenAI
     * 중요도 필터: ?important=HIGH
     *
     * 조회 순서 (어느 단계가 실패해도 다음 단계로 — 화면이 비지 않게):
     *   1. Redis 캐시 (오늘 성공본)
     *   2. DB에서 오늘 이하 가장 최근 성공본 (오늘 수집 실패 / 06:00 이전 / Redis 장애)
     *   3. 빈 응답
     */
    @GetMapping("/briefing")
    public ResponseEntity<ApiResponse<AiJournalDailyResponse>> getBriefing(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String important) {

        Optional<AiJournalDailyResponse> cached = readCache();
        boolean isCacheHit = cached.isPresent();

        AiJournalDailyResponse daily = cached
                .or(() -> archiveService.findLatestOnOrBefore(LocalDate.now(KST)))
                .orElseGet(AiJournalDailyResponse::empty);

        if (source != null || important != null) {
            daily = daily.withBriefing(filter(daily.briefing(), source, important));
        }

        return ResponseEntity.ok()
                .header("X-Cache", isCacheHit ? "HIT" : "MISS")
                .body(ApiResponse.ok(daily));
    }

    /** Redis 장애·파싱 실패·빈 캐시는 모두 empty로 — 예외를 밖으로 던지지 않음 */
    private Optional<AiJournalDailyResponse> readCache() {
        try {
            String json = redisTemplate.opsForValue().get(AiJournalScheduler.CACHE_KEY_BRIEFING);
            if (json == null) {
                return Optional.empty();
            }
            AiJournalDailyResponse parsed = objectMapper.readValue(json, AiJournalDailyResponse.class);
            return parsed.briefing().hasContent() ? Optional.of(parsed) : Optional.empty();
        } catch (Exception e) {
            log.warn("AI 저널 캐시 조회 실패 — DB fallback. cause={}", e.getMessage());
            return Optional.empty();
        }
    }

    private AiJournalBriefingResponse filter(AiJournalBriefingResponse b, String source, String important) {
        List<AiJournalBriefingResponse.AiNewsItem> filtered = b.items().stream()
                .filter(item -> source == null || item.source().contains(source))
                .filter(item -> important == null || important.equals(item.important()))
                .toList();
        return new AiJournalBriefingResponse(b.dailySummary(), b.highlightCount(), filtered, b.topKeywords());
    }
}