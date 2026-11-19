package com.ysh.domain.news.repository;

import com.ysh.domain.news.entity.NewsCategory;
import com.ysh.domain.news.entity.NewsFeed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NewsFeedRepository extends JpaRepository<NewsFeed, Long> {

    /**
     * URL 기반 멱등성 INSERT.
     * @return 1 = 새로 INSERT됨, 0 = 이미 존재해서 skip
     */
    @Modifying
    @Query(value = """
        INSERT INTO news_feeds (source, category, title, url, summary, published_at, fetched_at)
        VALUES (:source, :category, :title, :url, :summary, :publishedAt, :fetchedAt)
        ON CONFLICT (url) DO NOTHING
        """, nativeQuery = true)
    int insertIfNotExists(
            @Param("source") String source,
            @Param("category") String category,
            @Param("title") String title,
            @Param("url") String url,
            @Param("summary") String summary,
            @Param("publishedAt") LocalDateTime publishedAt,
            @Param("fetchedAt") LocalDateTime fetchedAt
    );

    @Query(value = """
        SELECT n FROM NewsFeed n WHERE n.category = :category ORDER BY n.publishedAt DESC""")
    List<NewsFeed> findLatestByCategory(@Param("category") NewsCategory category, Pageable pageable);

    // ─────────────── 보강(enrichment) ───────────────

    /**
     * 보강 대기 건 — 최신순. 시도 상한 미만 + 최근 기간만 (오래된 기사는 보강 가치 없음).
     * 부분 인덱스 idx_news_feeds_enrich_pending 사용.
     */
    @Query("""
        SELECT n FROM NewsFeed n
        WHERE n.enrichedAt IS NULL
          AND n.enrichAttempts < :maxAttempts
          AND n.category IN :categories
          AND n.publishedAt >= :since
        ORDER BY n.publishedAt DESC""")
    List<NewsFeed> findEnrichPending(@Param("maxAttempts") int maxAttempts,
                                     @Param("categories") Collection<NewsCategory> categories,
                                     @Param("since") LocalDateTime since,
                                     Pageable pageable);

    /**
     * 보강 결과 반영. enriched_at IS NULL 조건으로 중복 반영 방지(멱등).
     * null 바인딩 타입 추론 문제를 피하려고 CAST 명시.
     */
    @Modifying
    @Query(value = """
        UPDATE news_feeds
           SET title_ko    = CAST(:titleKo AS VARCHAR),
               keywords    = CAST(:keywords AS JSONB),
               enriched_at = :enrichedAt
         WHERE id = :id
           AND enriched_at IS NULL
        """, nativeQuery = true)
    int markEnriched(@Param("id") Long id,
                     @Param("titleKo") String titleKo,
                     @Param("keywords") String keywordsJson,
                     @Param("enrichedAt") LocalDateTime enrichedAt);

    /** 실패 건 시도 횟수 +1 */
    @Modifying
    @Query("""
        UPDATE NewsFeed n
           SET n.enrichAttempts = n.enrichAttempts + 1
         WHERE n.id IN :ids
           AND n.enrichedAt IS NULL""")
    int incrementEnrichAttempts(@Param("ids") Collection<Long> ids);

    // ─────────────── 수집 현황 ───────────────

    /** 기간 내 새로 저장된 기사 수 (fetched_at = INSERT 시각) */
    @Query("""
        SELECT COUNT(n) FROM NewsFeed n
        WHERE n.fetchedAt >= :since
          AND n.category IN :categories""")
    long countFetchedSince(@Param("since") LocalDateTime since,
                           @Param("categories") Collection<NewsCategory> categories);

    /** 기간 내 번역 완료 기사 수 */
    @Query("""
        SELECT COUNT(n) FROM NewsFeed n
        WHERE n.titleKo IS NOT NULL
          AND n.enrichedAt >= :since""")
    long countTranslatedSince(@Param("since") LocalDateTime since);

    /** ES 재색인 대상 — 기간 내 보강 완료 건 */
    @Query("""
        SELECT n FROM NewsFeed n
        WHERE n.enrichedAt IS NOT NULL
          AND n.publishedAt >= :since""")
    List<NewsFeed> findEnrichedSince(@Param("since") LocalDateTime since);
}
