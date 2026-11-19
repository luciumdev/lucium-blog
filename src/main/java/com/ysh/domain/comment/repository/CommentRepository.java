package com.ysh.domain.comment.repository;

import com.ysh.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Optional<Comment> findByPublicId(UUID publicId);

    // 최상위 댓글 + 대댓글 한 번에 조회 (N+1 방지)
    @Query("""
        SELECT c FROM Comment c
        LEFT JOIN FETCH c.parent
        WHERE c.post.id = :postId
        ORDER BY
            COALESCE(c.parent.id, c.id),
            c.id ASC
        """)
    List<Comment> findAllByPostId(@Param("postId") Long postId);

}
