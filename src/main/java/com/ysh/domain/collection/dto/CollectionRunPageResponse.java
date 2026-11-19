package com.ysh.domain.collection.dto;

import com.ysh.domain.collection.entity.RunStatus;
import com.ysh.domain.collection.entity.RunTrigger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * /status 실행 로그 한 페이지.
 * @param page 0부터 시작
 */
public record CollectionRunPageResponse(
        List<Item> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    /**
     * @param reason     공개용 사유 (분류·마스킹됨). 성공이면 null
     * @param rawMessage 원문 — 관리자일 때만 값이 있음
     */
    public record Item(
            long id,
            String job,
            String jobLabel,
            RunTrigger trigger,
            RunStatus status,
            LocalDateTime startedAt,
            long durationMs,
            int itemCount,
            String reason,
            String rawMessage
    ) {}
}
