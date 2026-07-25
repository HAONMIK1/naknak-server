package com.na.naknak.server.upload.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.upload.infrastructure.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadService {

    private static final int MAX_FILE_COUNT = 5;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final S3Uploader s3Uploader;

    public List<String> uploadImages(List<MultipartFile> files) {
        if (files.size() > MAX_FILE_COUNT) {
            throw new BusinessException(ErrorCode.TOO_MANY_FILES);
        }

        return files.stream()
                .map(this::validateAndUpload)
                .toList();
    }

    private String validateAndUpload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        return s3Uploader.upload(file);
    }
}
