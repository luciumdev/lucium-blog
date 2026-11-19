package com.ysh.infra.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class FileDeleteRequest {
    @NotBlank(message = "fileUrl 필수값입니다.")
    @Size(max= 1000, message = "fileUrl 너무 깁니다.")
    private String fileUrl;

}
