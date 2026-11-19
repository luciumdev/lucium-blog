package com.ysh.domain.collection.dto;

import com.ysh.domain.collection.entity.RunStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메인 "데이터 수집" 카드 응답.
 * @param todayArticles   오늘(KST) 새로 저장된 테크·경제 기사 수
 * @param todayTranslated 오늘 번역된 기사 수
 * @param successRate7d   최근 7일 성공률(%) — EMPTY 제외. 기록이 없으면 null
 * @param jobs            작업별 최근 실행 상태
 */
public record CollectionStatusResponse(
        long todayArticles,
        long todayTranslated,
        Double successRate7d,
        List<JobStatus> jobs
) {
    /**
     * @param recent 최근 실행 상태 — 과거 → 최신 순 (화면 칸 순서 그대로)
     */
    public record JobStatus(
            String job,
            String label,
            String cycle,
            LocalDateTime lastRunAt,
            RunStatus lastStatus,
            List<RunStatus> recent
    ) {}
}
