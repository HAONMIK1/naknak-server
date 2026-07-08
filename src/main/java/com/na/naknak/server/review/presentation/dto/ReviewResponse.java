package com.na.naknak.server.review.presentation.dto;

import com.na.naknak.server.review.domain.Review;
import com.na.naknak.server.review.domain.ReviewImage;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record ReviewResponse(
        Long id,
        Long restaurantId,
        String restaurantName,
        Long userId,
        String nickname,
        String content,
        int rating,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        List<String> imageUrls = review.getImages().stream()
                .sorted(Comparator.comparingInt(ReviewImage::getSortOrder))
                .map(ReviewImage::getImageUrl)
                .toList();
        return new ReviewResponse(
                review.getId(),
                review.getRestaurant().getId(),
                review.getRestaurant().getName(),
                review.getUser().getId(),
                review.getUser().getNickname(),
                review.getContent(),
                review.getRating(),
                imageUrls,
                review.getCreatedAt()
        );
    }
}
