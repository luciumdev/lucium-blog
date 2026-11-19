package com.ysh.infra.s3;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class PresignedUrlRequest {
    @NotBlank(message = "파일명은 필수값입니다.")
    @Size(max= 255, message = "파일명이 너무 깁니다.")
    private String fileName;
    @NotBlank(message = "콘텐츠 타입은 필수값입니다.")
    @Size(max= 100, message = "콘텐츠 타입이 너무 깁니다.")
    private String contentType;
}
