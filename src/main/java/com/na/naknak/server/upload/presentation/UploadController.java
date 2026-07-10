package com.na.naknak.server.upload.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.common.auth.LoginUser;
import com.na.naknak.server.upload.application.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping("/images")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @LoginUser Long userId,
            @RequestParam("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(ApiResponse.ok(uploadService.uploadImages(files)));
    }
}
