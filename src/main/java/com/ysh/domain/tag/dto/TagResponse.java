package com.ysh.domain.tag.dto;

import com.ysh.domain.tag.repository.TagWithCount;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagResponse {
    private Long id;
    private String name;
    private String slug;
    private long postCount;   // 공개 API: PUBLISHED 글 개수 (int → long)

    public static TagResponse from(TagWithCount t) {
        return TagResponse.builder()
                .id(t.id())
                .name(t.name())
                .slug(t.slug())
                .postCount(t.publishedCount())
                .build();
    }
}