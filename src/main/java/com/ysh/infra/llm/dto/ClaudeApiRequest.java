package com.ysh.infra.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClaudeApiRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        String system,
        List<Message> messages,
        double temperature
) {
    public record Message(String role, String content) {}
}
