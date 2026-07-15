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
        return new NaverPlaceResponse(
                title,
                item.category(),
                address,
                roadAddress,
                parseCoord(item.mapy()),
                parseCoord(item.mapx()),
                buildPlaceUrl(item.link(), title, roadAddress != null && !roadAddress.isBlank() ? roadAddress : address)
        );
    }

    /**
     * 지역검색 API의 link 필드는 "네이버 플레이스 페이지"가 아니라 업체가 등록한 자체 홈페이지
     * URL이라 대부분 비어있거나 엉뚱한 곳을 가리킨다. place.naver.com/map.naver.com 링크가
     * 아니면 신뢰하지 않고, 이름+주소로 네이버 지도 검색 URL을 대신 만들어 안내한다.
     * (지역검색 API는 무료 범위에서 실제 플레이스 ID를 제공하지 않아 완벽한 딥링크는 불가능하다.)
     */
    private String buildPlaceUrl(String link, String title, String address) {
        if (link != null && (link.contains("place.naver.com") || link.contains("map.naver.com"))) {
            return link;
        }
        String query = address != null && !address.isBlank() ? title + " " + address : title;
        // URLEncoder는 form-urlencoded 규칙이라 공백을 '+'로 바꾸는데, URL 경로 세그먼트에서는
        // '+'가 공백으로 해석되지 않아 검색어가 깨진다(예: "별미칭냉면+서울시..."를 그대로 검색해
        // 정확한 업체를 못 찾고 주소 위치로만 이동함). 경로 세그먼트는 UriComponentsBuilder로
        // 퍼센트 인코딩(공백 -> %20)해야 네이버 지도가 단일 업체를 정확히 찾아 place 상세로 이동한다.
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