package com.na.naknak.server.restaurant.domain.repository;

import com.na.naknak.server.restaurant.domain.SavedRestaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavedRestaurantRepository extends JpaRepository<SavedRestaurant, Long> {

    boolean existsByUserIdAndRestaurantId(Long userId, Long restaurantId);

    Optional<SavedRestaurant> findByUserIdAndRestaurantId(Long userId, Long restaurantId);

    Page<SavedRestaurant> findByUserId(Long userId, Pageable pageable);
}
