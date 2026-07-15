package com.na.naknak.server.restaurant.presentation.dto;

import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.restaurant.domain.RestaurantImage;

import java.util.Comparator;
import java.util.List;

public record RestaurantResponse(
        Long id,
        String name,
        String category,
        String address,
        Double latitude,
        Double longitude,
        String naverPlaceUrl,
        List<String> imageUrls
) {
    public static RestaurantResponse from(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getCategory(),
                restaurant.getAddress(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getNaverPlaceUrl(),
                restaurant.getImages().stream()
                        .sorted(Comparator.comparingInt(RestaurantImage::getSortOrder))
                        .map(RestaurantImage::getImageUrl)
                        .toList()
        );
    }
}