package com.na.naknak.server.restaurant.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.restaurant.domain.SavedRestaurant;
import com.na.naknak.server.restaurant.domain.repository.RestaurantRepository;
import com.na.naknak.server.restaurant.domain.repository.SavedRestaurantRepository;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantResponse;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavedRestaurantService {

    private final SavedRestaurantRepository savedRestaurantRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public void save(Long userId, Long restaurantId) {
        if (savedRestaurantRepository.existsByUserIdAndRestaurantId(userId, restaurantId)) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));
        savedRestaurantRepository.save(SavedRestaurant.create(user, restaurant));
    }

    @Transactional
    public void unsave(Long userId, Long restaurantId) {
        savedRestaurantRepository.findByUserIdAndRestaurantId(userId, restaurantId)
                .ifPresent(savedRestaurantRepository::delete);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> getSaved(Long userId, Pageable pageable) {
        return savedRestaurantRepository.findByUserId(userId, pageable)
                .map(saved -> RestaurantResponse.from(saved.getRestaurant()));
    }
}
