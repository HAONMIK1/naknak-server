package com.na.naknak.server.review.domain.repository;

import com.na.naknak.server.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRestaurantIdAndDeletedAtIsNull(Long restaurantId, Pageable pageable);

    Page<Review> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Page<Review> findByUserIdInAndDeletedAtIsNull(Collection<Long> userIds, Pageable pageable);
}
