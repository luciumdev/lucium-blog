package com.ysh.infra.aijournal;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 저널 일일 응답 (API 노출용 wrapper).
 *
 * - briefing: LLM이 생성한 본문 (LLM 응답 스키마 그대로)
 * - failedSources: 수집 단계에서 실패한 외부 소스 목록
 * - journalDate: 이 브리핑이 수집된 날짜 (KST). 오늘 수집이 실패해 이전 날짜를 내려줄 때
 *   프론트가 "MM/dd 수집본" 안내를 띄우는 데 사용. 구버전 캐시 JSON에는 없으므로 null 가능.
 *
 * LLM 출력 매핑(AiJournalBriefingResponse)과 사용자 응답을 분리해
 * "외부 시스템 응답 != API 응답" 경계를 명확히 한다.
 */
public record AiJournalDailyResponse(
        AiJournalBriefingResponse briefing,
        List<SourceFailure> failedSources,
        LocalDate journalDate
) {
    public AiJournalDailyResponse {
        briefing = briefing == null ? AiJournalBriefingResponse.empty() : briefing;
        failedSources = failedSources == null ? List.of() : failedSources;
    }

    public static AiJournalDailyResponse of(
            AiJournalBriefingResponse briefing,
            List<SourceFailure> failures,
            LocalDate journalDate
    ) {
        return new AiJournalDailyResponse(briefing, failures, journalDate);
    }

    public static AiJournalDailyResponse onlyBriefing(AiJournalBriefingResponse briefing, LocalDate journalDate) {
        return new AiJournalDailyResponse(briefing, List.of(), journalDate);
    }

    /** 기존 호출부 호환용 (날짜 미상) — 신규 코드는 날짜를 넘기는 오버로드 사용 */
    public static AiJournalDailyResponse of(AiJournalBriefingResponse briefing, List<SourceFailure> failures) {
        return new AiJournalDailyResponse(briefing, failures, null);
    }

    /** 기존 호출부 호환용 (날짜 미상) */
    public static AiJournalDailyResponse onlyBriefing(AiJournalBriefingResponse briefing) {
        return new AiJournalDailyResponse(briefing, List.of(), null);
    }

    /** 표시할 데이터가 아예 없을 때 */
    public static AiJournalDailyResponse empty() {
        return new AiJournalDailyResponse(AiJournalBriefingResponse.empty(), List.of(), null);
    }

    /** 필터 적용 시 briefing만 교체 */
    public AiJournalDailyResponse withBriefing(AiJournalBriefingResponse filtered) {
        return new AiJournalDailyResponse(filtered, failedSources, journalDate);
    }
}