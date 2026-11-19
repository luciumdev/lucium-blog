package com.ysh.infra.llm;

/**
 * 모델·토큰·temperature 는 용도별 Properties 로 외부화
 * ({@link ClaudeAiJournalProperties}, {@link ClaudeNewsEnrichmentProperties}).
 * @param systemPrompt
 * @param userPrompt
 * @param maxTokens
 * @param temperature
 * @param model
 */
public record ClaudePromptRequest(
        String systemPrompt,
        String userPrompt,
        int maxTokens,
        double temperature,
        String model
) {}
