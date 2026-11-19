package com.ysh.domain.post.repository;

import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {
    // 외부에서 게시글 조회 시 반드시 publicId + PUBLISHED 조건으로만 조회
    Optional<Post> findByPublicIdAndStatus(UUID publicId, PostStatus status);

    // 어드민용 — 상태 무관 조회 (DRAFT 포함)
    Optional<Post> findByPublicId(UUID publicId);

    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.category
        WHERE p.status = :status
        ORDER BY p.publishedAt DESC
        """)
    Page<Post> findAllByStatus(@Param("status") PostStatus status, Pageable pageable);

    @Query("""
        SELECT p FROM Post p
        JOIN p.postTags pt
        JOIN pt.tag t
        LEFT JOIN FETCH p.category
        WHERE t.slug = :tagSlug
          AND p.status = 'PUBLISHED'
        ORDER BY p.publishedAt DESC
        """)
    Page<Post> findAllByTagSlug(@Param("tagSlug") String tagSlug, Pageable pageable);

    @Query("""
        SELECT p FROM Post p
        LEFT JOIN FETCH p.category c
        WHERE c.slug = :categorySlug
          AND p.status = 'PUBLISHED'
        ORDER BY p.publishedAt DESC
        """)
    Page<Post> findAllByCategorySlug(@Param("categorySlug") String categorySlug, Pageable pageable);

    @Query("""
        SELECT p FROM Post p
        WHERE p.status = 'PUBLISHED'
        ORDER BY p.viewCount DESC
        """)
    Page<Post> findTopByViewCount(Pageable pageable);

    boolean existsByPublicId(UUID publicId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + :delta WHERE p.id = :id")
    int incrementViewCount(@Param("id") Long id, @Param("delta") long delta);
    @Query(value = """
    SELECT DISTINCT p FROM Post p
    LEFT JOIN FETCH p.category
    LEFT JOIN FETCH p.postTags pt
    LEFT JOIN FETCH pt.tag
    WHERE p.status = :status
    """,
            countQuery = "SELECT COUNT(p) FROM Post p WHERE p.status = :status")
    Page<Post> findAllByStatusForReindex(@Param("status") PostStatus status, Pageable pageable);

    long countByStatus(PostStatus postStatus);

    @Query("SELECT p.publicId FROM Post p WHERE p.status = :status")
    List<UUID> findPublicIdsByStatus(@Param("status") PostStatus status);

    @Query("""
    SELECT p FROM Post p
    LEFT JOIN FETCH p.category
    WHERE (:status IS NULL OR p.status = :status)
      AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Post> findAllForAdmin(@Param("status") PostStatus status, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.viewCount), 0) FROM Post p WHERE p.status = :status")
    long sumViewCountByStatus(@Param("status") PostStatus status);
}
