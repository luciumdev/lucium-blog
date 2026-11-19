package com.ysh.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CommentCreateRequest {
    @NotBlank(message = "작성자 이름은 필수입니다.")
    @Size(max = 50, message = "작성자 이름은 50자 이하여야 합니다.")
    private String authorName;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 4, max = 20, message = "비밀번호는 4~20자 이내여야 합니다.")
    private String password;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다.")
    private String content;

    // 대댓글인 경우만 설정 (null이면 최상위 댓글)
    private UUID parentPublicId;
}
