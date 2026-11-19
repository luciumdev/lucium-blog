package com.ysh.domain.comment.dto;

import com.ysh.domain.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CommentResponse {
    private UUID publicId;
    private String                authorName;
    private String                content;
    private boolean               deleted;
    private LocalDateTime createdAt;
    private List<CommentResponse> replies;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .publicId(comment.getPublicId())
                .authorName(comment.isDeleted() ? null : comment.getAuthorName())
                .content(comment.getContent())
                .deleted(comment.isDeleted())
                .createdAt(comment.getCreatedAt())
                .replies(new ArrayList<>())
                .build();
    }

    /**
     * 평탄한 리스트 → 트리 구조 변환 (1 depth만 허용)
     */
    public static List<CommentResponse> toTree(List<Comment> comments) {
        List<CommentResponse> roots = comments.stream()
                .filter(c -> c.getParent() == null)
                .map(CommentResponse::from)
                .toList();

        roots.forEach(root ->
                comments.stream()
                        .filter(c -> c.getParent() != null
                                && c.getParent().getPublicId().equals(root.getPublicId()))
                        .map(CommentResponse::from)
                        .forEach(root.getReplies()::add)
        );

        return roots;
    }
}
