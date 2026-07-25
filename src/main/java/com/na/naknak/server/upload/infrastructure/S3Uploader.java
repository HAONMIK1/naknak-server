package com.na.naknak.server.upload.infrastructure;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class S3Uploader {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final S3Client s3Client;
    private final String bucket;
    private final String cdnDomain;

    public S3Uploader(
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.region}") String region,
            @Value("${cdn.domain}") String cdnDomain
    ) {
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
        this.bucket = bucket;
        this.cdnDomain = cdnDomain;
    }

    public String upload(MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String key = "uploads/" + UUID.randomUUID() + "." + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException | software.amazon.awssdk.core.exception.SdkException e) {
            log.error("S3 업로드 실패", e);
            throw new BusinessException(ErrorCode.S3_UPLOAD_ERROR);
        }

        return cdnDomain + "/" + key;
    }

    private String extractExtension(String originalFilename) {
        String ext = originalFilename == null ? "" : originalFilename
                .substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext) ? ext : "jpg";
    }
}
