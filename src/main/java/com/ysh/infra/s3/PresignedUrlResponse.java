package com.ysh.infra.s3;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponse {
    private final String uploadUrl;
    private final String fileUrl;
    private final long   expiresIn;
}
