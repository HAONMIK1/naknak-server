package com.na.naknak.server.review.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.common.auth.LoginUser;
import com.na.naknak.server.review.application.ReviewService;
import com.na.naknak.server.review.presentation.dto.ReviewCreateRequest;
import com.na.naknak.server.review.presentation.dto.ReviewResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/v1/restaurants/{restaurantId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @LoginUser Long userId,
            @PathVariable Long restaurantId,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.create(userId, restaurantId, request)));
    }

    @GetMapping("/api/v1/restaurants/{restaurantId}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getRestaurantReviews(
            @PathVariable Long restaurantId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getRestaurantReviews(restaurantId, pageable)));
    }

    @GetMapping("/api/v1/users/me/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getMyReviews(
            @LoginUser Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getMyReviews(userId, pageable)));
    }

    @DeleteMapping("/api/v1/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @LoginUser Long userId,
            @PathVariable Long reviewId
    ) {
        reviewService.delete(userId, reviewId);
        return ResponseEntity.ok(ApiResponse.ok("리뷰가 삭제되었습니다."));
    }
}
