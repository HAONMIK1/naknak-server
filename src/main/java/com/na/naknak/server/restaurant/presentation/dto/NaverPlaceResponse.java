package com.na.naknak.server.restaurant.presentation.dto;

/**
 * 네이버 검색 결과를 가공한 맛집 후보. 클라이언트가 이 값을 그대로 등록 요청에 사용한다.
 */
public record NaverPlaceResponse(
        String name,
        String category,
        String address,
        String roadAddress,
        Double latitude,
        Double longitude,
        String placeUrl
) {}