package com.ysh.domain.post.dto;

import com.ysh.domain.post.entity.ContentFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class PostUpdateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 300, message = "제목은 300자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "본문은 필수입니다.")
    private String content;

    // null이면 기존 형식 유지
    private ContentFormat contentFormat;

    @Size(max = 500, message = "요약은 500자 이하여야 합니다.")
    private String summary;

    private String thumbnail;

    private Long categoryId;

    private List<String> tags;

}
