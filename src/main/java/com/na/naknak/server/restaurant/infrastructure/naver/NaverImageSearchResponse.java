package com.na.naknak.server.restaurant.infrastructure.naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** 네이버 이미지 검색 API 원본 응답 매핑. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverImageSearchResponse(
        List<Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String link,
            String thumbnail
    ) {}
}
