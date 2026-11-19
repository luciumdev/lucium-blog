package com.ysh.domain.news.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 뉴스 피드.
 *
 * <p>Redis 캐시에 엔티티 그대로 직렬화되므로 파생 getter(계산 프로퍼티)를 추가하지 말 것.
 * 필드가 없는 getter 가 생기면 역직렬화 시 unknown property 로 실패한다.
 *
 * <p>보강 컬럼(titleKo, keywords, enrichedAt, enrichAttempts)은 엔티티 setter 대신
 * {@code NewsFeedRepository} 의 벌크 UPDATE 로만 변경한다 (보강은 별도 단계, 수집 트랜잭션과 분리).
 */
@Entity
@Table(name = "news_feeds")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class NewsFeed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NewsCategory category;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 1000)
    private String url;

    // HN은 null. AI 저널에서 LLM 요약 채움.
    @Column(columnDefinition = "TEXT")
    private String summary;

    /** KST 기준 게시 시각 */
    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    /** 영어 제목 번역 (IT만). 경제 뉴스·미보강은 null */
    @Column(name = "title_ko", length = 500)
    private String titleKo;

    /** 정규화 키워드 JSON 배열 문자열. 예: ["반도체","엔비디아"] */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private String keywords;

    @Column(name = "enriched_at")
    private LocalDateTime enrichedAt;

    @Column(name = "enrich_attempts", nullable = false)
    private int enrichAttempts;

    @Builder
    private NewsFeed(String source, NewsCategory category,
                     String title, String url,
                     String summary, LocalDateTime publishedAt) {
        this.source = source;
        this.category = category;
        this.title = title;
        this.url = url;
        this.summary = summary;
        this.publishedAt = publishedAt;
        this.fetchedAt = LocalDateTime.now();
    }
}
