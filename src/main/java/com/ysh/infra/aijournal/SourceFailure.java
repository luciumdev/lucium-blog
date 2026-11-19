package com.ysh.infra.aijournal;

/**
 * 수집 실패 1건.
 *
 * failedAt은 ISO-8601 문자열로 저장한다 (Jackson 2/3 혼재 환경에서
 * Instant 직렬화 모듈 의존을 피하려는 의도 — 메모리 잔재 정리 전 안전책).
 */
public record SourceFailure(
        String source,    // "Anthropic", "GitHub:openai/openai-python", "HackerNews"
        String reason,    // "404 Not Found", "Connection timeout", ...
        String failedAt   // ISO-8601 (e.g. "2026-05-08T23:04:05Z")
) {}