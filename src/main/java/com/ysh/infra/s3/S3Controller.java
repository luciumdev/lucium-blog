package com.ysh.infra.s3;

import com.ysh.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/presigned-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        PresignedUrlResponse response= s3Service.generatePresignedUrl(request.getFileName(), request.getContentType());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@Valid @RequestBody FileDeleteRequest request) {
        s3Service.deleteObject(request.getFileUrl());
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
