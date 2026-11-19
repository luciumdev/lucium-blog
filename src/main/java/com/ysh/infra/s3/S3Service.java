package com.ysh.infra.s3;

import com.ysh.common.exception.BusinessException;
import com.ysh.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of( "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm","application/pdf");

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.cloudfront.base-url}")
    private String cdnBaseUrl;

    @Value("${aws.s3.presigned-url-expiration}")
    private long presignedUrlExpiration;

    /**
     *  Presigned PUT URL 발급
     * @param originalFileName
     * @param ContentType
     * @return
     */
    public PresignedUrlResponse generatePresignedUrl (String originalFileName, String ContentType) {
        validateContentType(ContentType);

        String key = buildObjectKey(originalFileName);
        String fileUrl = cdnBaseUrl + "/" + key;

        PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignedUrlExpiration))
                .putObjectRequest(r -> r
                        .bucket(bucket)
                        .key(key)
                        .contentType(ContentType))
                .build();

        PresignedPutObjectRequest presignRequest = s3Presigner.presignPutObject(putObjectPresignRequest);

        return PresignedUrlResponse.builder()
                .uploadUrl(presignRequest.url().toString())
                .fileUrl(fileUrl)
                .expiresIn(presignedUrlExpiration)
                .build();
    }

    /**
     * 업로드 파일 삭제
     * @param fileUrl
     */
    public void deleteObject(String fileUrl) {
        String key = extractKey(fileUrl);
        try{
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (Exception e) {
            log.error("S3 object delete failed. key={}", key, e);
            throw new BusinessException(ErrorCode.FILE_DELETE_FAILED);
        }
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    /**
     * s3 object key 생성
     * @param originalFileName
     * @return
     */
    private String buildObjectKey(String originalFileName) {
        String ext = "";
        if (originalFileName != null) {
            int dotIndex = originalFileName.lastIndexOf(".");
            if (dotIndex >= 0) {
                ext = originalFileName.substring(dotIndex).toLowerCase();
            }
        }
        return "uploads/" + UUID.randomUUID() + ext;
    }

    private String extractKey(String fileUrl) {
        String prefix = cdnBaseUrl + "/";
        if (fileUrl == null || !fileUrl.startsWith(prefix)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "fileUrl must start with " + prefix);
        }
        return fileUrl.substring(cdnBaseUrl.length() + 1);
    }
}
