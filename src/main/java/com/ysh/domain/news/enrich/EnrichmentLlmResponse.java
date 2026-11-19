package com.ysh.domain.news.enrich;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * LLM 출력 스키마.
 * {"items":[{"id":123,"title_ko":"..." | null,"keywords":["반도체"]}]}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record EnrichmentLlmResponse(List<Item> items) {

    EnrichmentLlmResponse {
        items = items == null ? List.of() : items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(
            Long id,
            @JsonProperty("title_ko") String titleKo,
            List<String> keywords
    ) {}
}
