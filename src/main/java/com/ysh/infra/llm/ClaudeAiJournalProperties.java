package com.ysh.infra.llm;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("claude.ai-journal-briefing")
public record ClaudeAiJournalProperties(
        @NotBlank String model,
        @Positive int maxTokens,
        @PositiveOrZero @DecimalMax("2.0") double temperature
) {}
