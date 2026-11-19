package com.ysh.infra.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaudeApiResponse (
        String id,
        String model,
        String role,
        List<ContentBlock> content,
        @JsonProperty("stop_reason") String stopReason,
        Usage usage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentBlock(String type, String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(   @JsonProperty("input_tokens") int inputTokens,
                           @JsonProperty("output_tokens") int outputTokens) {}
    public String firstText(){
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.stream()
                .filter(c ->"text".equals(c.type))
                .map(ContentBlock::text)
                .findFirst()
                .orElse("");
    }
}
