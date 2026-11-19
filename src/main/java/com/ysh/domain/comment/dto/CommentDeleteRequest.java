package com.ysh.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CommentDeleteRequest {
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
