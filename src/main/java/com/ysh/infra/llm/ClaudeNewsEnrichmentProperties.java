package com.ysh.infra.llm;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 뉴스 보강(제목 번역 + 키워드 정규화) LLM 설정.
 * 창작이 아니라 변환 작업이라 저가 모델 + temperature 0 권장.
 */
@Validated
@ConfigurationProperties("claude.news-enrichment")
public record ClaudeNewsEnrichmentProperties(
        @NotBlank String model,
        @Positive int maxTokens,
        @PositiveOrZero @DecimalMax("2.0") double temperature
) {}
