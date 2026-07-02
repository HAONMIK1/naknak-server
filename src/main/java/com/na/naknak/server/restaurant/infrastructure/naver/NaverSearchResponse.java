package com.na.naknak.server.restaurant.infrastructure.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 네이버 지역검색 API 원본 응답 매핑.
 * mapx/mapy 는 WGS84 좌표 x 1e7 형태의 문자열이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverSearchResponse(
        List<Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String title,
            String link,
            String category,
            String address,
            String roadAddress,
            String mapx,
            String mapy
    ) {}
}