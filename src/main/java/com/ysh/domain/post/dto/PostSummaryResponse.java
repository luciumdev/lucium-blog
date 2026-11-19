package com.ysh.domain.post.dto;

import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.entity.PostStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PostSummaryResponse {
    private UUID publicId;
    private String         title;
    private String         summary;
    private String         thumbnail;
    private String         categoryName;
    private List<String> tags;
    private long           viewCount;
    private PostStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime  createdAt;

    public static PostSummaryResponse from(Post post) {
        return from(post, post.getViewCount());
    }

    public static PostSummaryResponse from(Post post, long viewCount) {
        return PostSummaryResponse.builder()
                .publicId(post.getPublicId())
                .title(post.getTitle())
                .summary(post.getSummary())
                .thumbnail(post.getThumbnail())
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
                .tags(post.getPostTags().stream()
                        .map(pt -> pt.getTag().getName())
                        .toList())
                .viewCount(viewCount)
                .status(post.getStatus())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
