package com.ysh.infra.aijournal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

/**
 * LLM 브리핑 응답.
 *
 * LLM 출력은 스키마를 어길 수 있다 (필드 누락 → null).
 * null이 그대로 캐시·DB·프론트까지 흘러가면 프론트에서 `.length` 접근 시 화면이 깨지므로
 * compact constructor에서 기본값으로 정규화한다. (역직렬화 시에도 생성자를 타므로 캐시 데이터도 보정됨)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiJournalBriefingResponse(

        @JsonProperty("daily_summary")
        String dailySummary,

        @JsonProperty("highlight_count")
        HighlightCount highlightCount,

        List<AiNewsItem> items,

        @JsonProperty("top_keywords")
        List<String> topKeywords

) {
    public AiJournalBriefingResponse {
        dailySummary = dailySummary == null ? "" : dailySummary;
        items = items == null ? List.of() : items;
        topKeywords = topKeywords == null ? List.of() : topKeywords;
        highlightCount = highlightCount == null ? HighlightCount.from(items) : highlightCount;
    }

    public static AiJournalBriefingResponse empty() {
        return new AiJournalBriefingResponse(
                "오늘 수집된 AI 업데이트가 없습니다.",
                new HighlightCount(0, 0, 0, 0),
                List.of(),
                List.of()
        );
    }

    /**
     * 화면에 보여줄 내용이 있는지 — 스케줄러가 "실패 결과로 기존 데이터를 덮어쓰지 않기" 판단에 사용.
     * is/get 접두어를 피하고 @JsonIgnore까지 달아 Jackson이 필드로 직렬화하지 않게 함.
     */
    @JsonIgnore
    public boolean hasContent() {
        return !items.isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HighlightCount(
            int total,
            int high,
            int medium,
            int low
    ) {
        static HighlightCount from(List<AiNewsItem> items) {
            int high = 0, medium = 0, low = 0;
            for (AiNewsItem item : items) {
                switch (item.important()) {
                    case "HIGH" -> high++;
                    case "MEDIUM" -> medium++;
                    default -> low++;
                }
            }
            return new HighlightCount(items.size(), high, medium, low);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiNewsItem(
            String source,
            String title,
            String summary,
            String important,   // HIGH | MEDIUM | LOW
            String category,
            List<String> tags,
            String url
    ) {
        private static final Set<String> IMPORTANCE = Set.of("HIGH", "MEDIUM", "LOW");

        public AiNewsItem {
            source = source == null ? "" : source;
            title = title == null ? "" : title;
            summary = summary == null ? "" : summary;
            important = important != null && IMPORTANCE.contains(important) ? important : "LOW";
            category = category == null ? "기타" : category;
            tags = tags == null ? List.of() : tags;
            url = url == null ? "" : url;
        }
    }
}