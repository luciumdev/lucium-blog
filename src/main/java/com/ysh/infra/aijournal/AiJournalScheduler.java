package com.ysh.infra.aijournal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ysh.domain.aijournal.service.AiJournalArchiveService;
import com.ysh.domain.collection.entity.CollectionJob;
import com.ysh.domain.collection.entity.RunOutcome;
import com.ysh.domain.collection.entity.RunTrigger;
import com.ysh.domain.collection.service.CollectionRunRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * AI 저널 수집 스케줄러
 *
 * 수집 주기:
 * - RSS/API 수집: 매일 오전 6시
 * - LLM 브리핑 생성: 수집 직후 1회 (하루 1회 — API 비용 최소화)
 *
 * 실행 결과는 collection_runs 에 기록 (메인 "데이터 수집" 카드):
 *   SUCCESS / PARTIAL(일부 소스 실패) / FAILED(수집 0건·LLM 결과 없음)
 *
 * 실패 정책 (이전 내용 유지):
 * - 수집 0건 / LLM 실패·빈 응답 / 파싱 실패 → DB·캐시를 갱신하지 않음.
 *   빈 결과로 덮어쓰면 화면이 "없음"이 되므로, 조회 시 컨트롤러가 가장 최근 성공본을 내려준다.
 *
 * 비용 계산:
 * - 하루 24건 기사 × 평균 300 토큰 = 7,200 input tokens
 * - Output 약 1,500 tokens
 * - claude-sonnet-4-6 기준 약 $0.01/일 = $0.30/월
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiJournalScheduler {

    static final String CACHE_KEY_BRIEFING = "ai:journal:briefing";
    private static final String CACHE_KEY_NEWS = "ai:journal:news";
    private static final Duration CACHE_TTL = Duration.ofHours(25);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final AiJournalRssCollector rssCollector;
    private final AiJournalBriefingService briefingService;
    private final AiJournalArchiveService archiveService;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CollectionRunRecorder recorder;

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    public void collectAndBrief() {
        recorder.run(CollectionJob.AI_JOURNAL, RunTrigger.SCHEDULED, this::runOnce);
    }

    /** 1회 실행 — 결과 판정까지. 처리하지 못한 예외는 기록기가 FAILED 로 남긴다. */
    RunOutcome runOnce() {
        log.info("AI 저널 일일 수집 시작");

        CollectionResult result = rssCollector.collectAll();
        if (result.news().isEmpty()) {
            log.warn("수집된 AI 뉴스 없음 — 이전 브리핑 유지 (실패 소스: {}개)", result.failures().size());
            return RunOutcome.failed("수집 0건 (실패 소스 " + result.failures().size() + "개)");
        }

        try {
            // 1. 수집 원본 캐시
            redisTemplate.opsForValue().set(
                    CACHE_KEY_NEWS,
                    objectMapper.writeValueAsString(result.news()),
                    CACHE_TTL
            );

            // 2. LLM 호출 — failures는 LLM에 전달하지 않음 (LLM 입력은 컨텐츠만)
            AiJournalBriefingResponse briefing = briefingService.generateDailyBriefing(result.news());

            // LLM 실패·빈 응답·파싱 실패는 모두 빈 브리핑으로 돌아옴 → 덮어쓰지 않고 종료
            if (!briefing.hasContent()) {
                log.warn("LLM 브리핑 결과 없음 — 이전 브리핑 유지 (수집 {}건)", result.news().size());
                return RunOutcome.failed("LLM 결과 없음 (수집 " + result.news().size() + "건)");
            }

            // 3. DB 영속화 — failedSources는 운영성 정보라 DB 미저장
            archiveService.persist(briefing);

            // 4. Redis 캐시는 wrapping된 daily 응답으로 저장
            AiJournalDailyResponse daily = AiJournalDailyResponse.of(
                    briefing, result.failures(), LocalDate.now(KST));
            redisTemplate.opsForValue().set(
                    CACHE_KEY_BRIEFING,
                    objectMapper.writeValueAsString(daily),
                    CACHE_TTL
            );

            log.info("AI 저널 완료 - 기사 {}건, HIGH {}건, 실패 소스 {}개",
                    briefing.highlightCount().total(),
                    briefing.highlightCount().high(),
                    result.failures().size());

            int count = briefing.highlightCount().total();
            return result.failures().isEmpty()
                    ? RunOutcome.success(count)
                    : RunOutcome.partial(count, "실패 소스 " + result.failures().size() + "개");
        } catch (Exception e) {
            log.error("AI 저널 브리핑 생성 실패 — 이전 브리핑 유지", e);
            return RunOutcome.failed(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
