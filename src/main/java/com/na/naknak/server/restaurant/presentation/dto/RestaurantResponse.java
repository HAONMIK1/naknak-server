package com.na.naknak.server.restaurant.presentation.dto;

import com.na.naknak.server.restaurant.domain.Restaurant;

public record RestaurantResponse(
        Long id,
        String name,
        String category,
        String address,
        Double latitude,
        Double longitude,
        String naverPlaceUrl
) {
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCategory(),
                restaurant.getAddress(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getNaverPlaceUrl()
        );
    }
}