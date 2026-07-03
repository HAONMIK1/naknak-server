package com.na.naknak.server.review.presentation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ReviewCreateRequest(
        @NotBlank(message = "리뷰 내용은 필수입니다.")
        String content,
        @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5 이하여야 합니다.")
        int rating,
        List<String> imageUrls
) {}