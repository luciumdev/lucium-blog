package com.ysh.domain.tag.repository;

import com.ysh.domain.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    Optional<Tag> findBySlug(String slug);

    /**
     * 공개 태그 목록 — PUBLISHED 글이 1개 이상인 태그만,
     * 발행 글 개수(publishedCount) 내림차순.
     * INNER JOIN이라 발행 글 없는 태그는 자동 제외된다.
     */
    @Query("""
            SELECT new com.ysh.domain.tag.repository.TagWithCount(
                t.id, t.name, t.slug, COUNT(pt)
            )
            FROM Tag t
            JOIN PostTag pt ON pt.tag = t
            WHERE pt.post.status = com.ysh.domain.post.entity.PostStatus.PUBLISHED
            GROUP BY t.id, t.name, t.slug
            ORDER BY COUNT(pt) DESC
            """)
    List<TagWithCount> findAllWithPublishedPosts();
}