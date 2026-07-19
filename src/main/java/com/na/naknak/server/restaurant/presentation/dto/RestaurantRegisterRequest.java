package com.na.naknak.server.restaurant.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record RestaurantRegisterRequest(
        @NotBlank(message = "맛집 이름은 필수입니다.")
        String name,
        @NotBlank(message = "카테고리는 필수입니다.")
        String category,
        @NotBlank(message = "주소는 필수입니다.")
        String address,
        Double latitude,
        Double longitude,
        String naverPlaceUrl
) {}