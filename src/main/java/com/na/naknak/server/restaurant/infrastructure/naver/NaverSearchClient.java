package com.na.naknak.server.restaurant.infrastructure.naver;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.restaurant.presentation.dto.NaverPlaceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class NaverSearchClient {

    private static final String LOCAL_SEARCH_URL = "https://openapi.naver.com/v1/search/local.json";
    private static final String IMAGE_SEARCH_URL = "https://openapi.naver.com/v1/search/image.json";
    private static final int DISPLAY = 5;
    private static final double COORD_SCALE = 1e7;

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public NaverSearchClient(
            @Value("${naver.client-id}") String clientId,
            @Value("${naver.client-secret}") String clientSecret
    ) {
        this.restClient = RestClient.create();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public List<NaverPlaceResponse> search(String query) {
        String uri = UriComponentsBuilder.fromHttpUrl(LOCAL_SEARCH_URL)
                .queryParam("query", query)
                .queryParam("display", DISPLAY)
                .build()
                .toUriString();

        NaverSearchResponse response = restClient.get()
                .uri(uri)
                .header("X-Naver-Client-Id", clientId)
                .header("X-Naver-Client-Secret", clientSecret)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new BusinessException(ErrorCode.NAVER_API_ERROR);
                })
                .body(NaverSearchResponse.class);

        if (response == null || response.items() == null) {
            return Collections.emptyList();
        }

        return response.items().stream()
                .map(this::toPlace)
                .toList();
    }

    /**
     * 지역검색 API는 사진을 내려주지 않아서, 맛집이 새로 등록될 때 대표 사진을 대신 찾아오는 용도로
     * 이미지 검색 API를 별도 호출한다. 사진은 부가 정보라 실패해도 등록 자체를 막으면 안 되므로,
     * 에러가 나면 예외를 던지지 않고 빈 목록을 반환한다.
     */
    public List<String> searchImages(String query, int count) {
        String uri = UriComponentsBuilder.fromHttpUrl(IMAGE_SEARCH_URL)
                .queryParam("query", query)
                .queryParam("display", count)
                .queryParam("sort", "sim")
                .build()
                .toUriString();

        try {
            NaverImageSearchResponse response = restClient.get()
                    .uri(uri)
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new BusinessException(ErrorCode.NAVER_API_ERROR);
                    })
                    .body(NaverImageSearchResponse.class);

            if (response == null || response.items() == null) {
                return Collections.emptyList();
            }
            return response.items().stream()
                    .map(NaverImageSearchResponse.Item::link)
                    .filter(link -> link != null && !link.isBlank())
                    .limit(count)
                    .toList();
        } catch (RestClientException | BusinessException e) {
            log.warn("네이버 이미지 검색 실패 (query={}): {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    private NaverPlaceResponse toPlace(NaverSearchResponse.Item item) {
        String title = stripTags(item.title());
        String address = item.address();
        String roadAddress = item.roadAddress();
        Double lat = parseCoord(item.mapy());
        Double lng = parseCoord(item.mapx());
        return new NaverPlaceResponse(
                title,
                item.category(),
                address,
                roadAddress,
                lat,
                lng,
                buildPlaceUrl(item.link(), title, lat, lng, roadAddress != null && !roadAddress.isBlank() ? roadAddress : address)
        );
    }

    /**
     * 지역검색 API의 link 필드는 "네이버 플레이스 페이지"가 아니라 업체가 등록한 자체 홈페이지
     * URL이라 대부분 비어있거나 엉뚱한 곳을 가리킨다. place.naver.com/map.naver.com 링크가
     * 아니면 신뢰하지 않는다. 이 경우 이름+주소로 만든 검색 URL은 동명이인/유사 상호가 있으면
     * 엉뚱한 검색 결과 목록으로 빠질 수 있어서, 좌표(mapx/mapy)가 있으면 그 좌표에 정확히 핀을
     * 꽂아주는 `map.naver.com/?lat=&lng=&title=` 형태를 우선 쓴다(비공식이지만 커뮤니티에서
     * 검증된 형식 — 좌표 기반이라 텍스트 매칭 실패 위험이 없다). 좌표가 없을 때만 텍스트 검색으로
     * 폴백한다. (지역검색 API는 무료 범위에서 실제 플레이스 ID를 제공하지 않아 완벽한 딥링크는
     * 애초에 불가능한 구조적 한계 — place.naver.com 링크가 있을 때만 진짜 플레이스 페이지로 간다.)
     */
    private String buildPlaceUrl(String link, String title, Double lat, Double lng, String address) {
        if (link != null && (link.contains("place.naver.com") || link.contains("map.naver.com"))) {
            return link;
        }
        if (lat != null && lng != null) {
            return UriComponentsBuilder.fromUriString("https://map.naver.com/")
                    .queryParam("lat", lat)
                    .queryParam("lng", lng)
                    .queryParam("title", title)
                    .build()
                    .encode()
                    .toUriString();
        }
        String query = address != null && !address.isBlank() ? title + " " + address : title;
        // URLEncoder는 form-urlencoded 규칙이라 공백을 '+'로 바꾸는데, URL 경로 세그먼트에서는
        // '+'가 공백으로 해석되지 않아 검색어가 깨진다. 경로 세그먼트는 UriComponentsBuilder로
        // 퍼센트 인코딩(공백 -> %20)해야 한다.
        return UriComponentsBuilder.fromUriString("https://map.naver.com/p/search/{query}")
                .buildAndExpand(query)
                .encode()
                .toUriString();
    }

    private String stripTags(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("<[^>]*>", "").trim();
    }

    private Double parseCoord(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw) / COORD_SCALE;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}