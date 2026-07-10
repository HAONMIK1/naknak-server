package com.na.naknak.server.review.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.restaurant.domain.repository.RestaurantRepository;
import com.na.naknak.server.review.domain.Review;
import com.na.naknak.server.review.domain.repository.ReviewRepository;
import com.na.naknak.server.review.presentation.dto.ReviewCreateRequest;
import com.na.naknak.server.review.presentation.dto.ReviewResponse;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public ReviewResponse create(Long userId, Long restaurantId, ReviewCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        Review review = Review.create(user, restaurant, request.content(), request.rating());

        List<String> imageUrls = request.imageUrls();
        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.size(); i++) {
                review.addImage(imageUrls.get(i), i);
            }
        }

        reviewRepository.save(review);
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getRestaurantReviews(Long restaurantId, Pageable pageable) {
        return reviewRepository.findByRestaurantIdAndDeletedAtIsNull(restaurantId, pageable)
                .map(ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(ReviewResponse::from);
    }

    @Transactional
    public ReviewResponse update(Long userId, Long reviewId, ReviewCreateRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        review.update(request.content(), request.rating());

        review.clearImages();
        List<String> imageUrls = request.imageUrls();
        if (imageUrls != null) {
            for (int i = 0; i < imageUrls.size(); i++) {
                review.addImage(imageUrls.get(i), i);
            }
        }

        return ReviewResponse.from(review);
    }

    @Transactional
    public void delete(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        review.delete();
    }
}
