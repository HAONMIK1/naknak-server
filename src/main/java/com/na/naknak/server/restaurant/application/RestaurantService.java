package com.na.naknak.server.restaurant.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.restaurant.domain.repository.RestaurantRepository;
import com.na.naknak.server.restaurant.infrastructure.naver.NaverSearchClient;
import com.na.naknak.server.restaurant.presentation.dto.NaverPlaceResponse;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantRegisterRequest;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final NaverSearchClient naverSearchClient;

    @Transactional(readOnly = true)
    public List<NaverPlaceResponse> searchNaver(String query) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return naverSearchClient.search(query);
    }

    private static final int REPRESENTATIVE_IMAGE_COUNT = 3;

    @Transactional
    public RestaurantResponse register(RestaurantRegisterRequest request) {
        Restaurant restaurant = restaurantRepository
                .findByNameAndAddress(request.name(), request.address())
                .orElseGet(() -> {
                    Restaurant created = restaurantRepository.save(Restaurant.create(
                            null,
                            request.naverPlaceUrl(),
                            request.name(),
                            request.category(),
                            request.address(),
                            request.latitude(),
                            request.longitude()
                    ));
                    attachNaverImages(created);
                    return created;
                });
        return RestaurantResponse.from(restaurant);
    }

    private void attachNaverImages(Restaurant restaurant) {
        List<String> imageUrls = naverSearchClient.searchImages(
                restaurant.getName() + " " + restaurant.getAddress(), REPRESENTATIVE_IMAGE_COUNT
        );
        for (int i = 0; i < imageUrls.size(); i++) {
            restaurant.addImage(imageUrls.get(i), i);
        }
    }

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return restaurantRepository.findByNameContaining(keyword, pageable)
                .map(RestaurantResponse::from);
    }

    @Transactional(readOnly = true)
    public RestaurantResponse getDetail(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));
        return RestaurantResponse.from(restaurant);
    }
}