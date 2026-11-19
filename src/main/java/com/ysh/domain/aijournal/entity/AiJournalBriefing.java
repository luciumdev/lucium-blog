package com.ysh.domain.aijournal.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ai_journal_briefings")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AiJournalBriefing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "journal_date", nullable = false, unique = true)
    private LocalDate journalDate;

    @Column(name = "daily_summary", nullable = false, columnDefinition = "text")
    private String dailySummary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_json", columnDefinition = "jsonb", nullable = false)
    private String itemsJson;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "top_keywords", columnDefinition = "text[]", nullable = false)
    private List<String> topKeywords;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "high_count", nullable = false)
    private int highCount;

    @Column(name = "medium_count", nullable = false)
    private int mediumCount;

    @Column(name = "low_count", nullable = false)
    private int lowCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    private AiJournalBriefing(LocalDate journalDate, String dailySummary, String itemsJson,
                              List<String> topKeywords, int totalCount,
                              int highCount, int mediumCount, int lowCount) {
        this.journalDate = journalDate;
        this.dailySummary = dailySummary;
        this.itemsJson = itemsJson;
        this.topKeywords = topKeywords;
        this.totalCount = totalCount;
        this.highCount = highCount;
        this.mediumCount = mediumCount;
        this.lowCount = lowCount;
    }

    /**
     * dirty checking update
     * @param dailySummary
     * @param itemsJson
     * @param topKeywords
     * @param totalCount
     * @param highCount
     * @param mediumCount
     * @param lowCount
     */
    public void updateFrom(String dailySummary, String itemsJson, List<String> topKeywords,
                           int totalCount, int highCount, int mediumCount, int lowCount) {
        this.dailySummary = dailySummary;
        this.itemsJson = itemsJson;
        this.topKeywords = topKeywords;
        this.totalCount = totalCount;
        this.highCount = highCount;
        this.mediumCount = mediumCount;
        this.lowCount = lowCount;
    }
}
