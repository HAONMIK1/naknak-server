package com.na.naknak.server.restaurant.infrastructure.naver;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.restaurant.presentation.dto.NaverPlaceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Component
public class NaverSearchClient {

    private static final String LOCAL_SEARCH_URL = "https://openapi.naver.com/v1/search/local.json";
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

    private NaverPlaceResponse toPlace(NaverSearchResponse.Item item) {
        return new NaverPlaceResponse(
                stripTags(item.title()),
                item.category(),
                item.address(),
                item.roadAddress(),
                parseCoord(item.mapy()),
                parseCoord(item.mapx()),
                item.link()
        );
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