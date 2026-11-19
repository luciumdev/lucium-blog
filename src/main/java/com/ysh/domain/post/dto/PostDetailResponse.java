package com.ysh.domain.post.dto;

import com.ysh.domain.post.entity.ContentFormat;
import com.ysh.domain.post.entity.Post;
import com.ysh.domain.post.entity.PostStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PostDetailResponse {

    private UUID           publicId;
    private String         title;
    private String         content;
    private ContentFormat  contentFormat;
    private String         summary;
    private String         thumbnail;
    private Long           categoryId;
    private String         categoryName;
    private List<String>   tags;
    private long           viewCount;
    private PostStatus     status;
    private LocalDateTime  publishedAt;
    private LocalDateTime  createdAt;
    private LocalDateTime  updatedAt;
    @Setter
    private boolean alreadyRead;

    public static PostDetailResponse from(Post post) {
        return from(post, post.getViewCount());
    }

    public static PostDetailResponse from(Post post, long viewCount) {
        return PostDetailResponse.builder()
                .publicId(post.getPublicId())
                .title(post.getTitle())
                .content(post.getContent())
                .contentFormat(post.getContentFormat())
                .summary(post.getSummary())
                .thumbnail(post.getThumbnail())
                .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : null)
                .tags(post.getPostTags().stream()
                        .map(pt -> pt.getTag().getName())
                        .toList())
                .viewCount(viewCount)
                .status(post.getStatus())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
