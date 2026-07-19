package com.na.naknak.server.restaurant.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.restaurant.application.RestaurantService;
import com.na.naknak.server.restaurant.presentation.dto.NaverPlaceResponse;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantRegisterRequest;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/search/naver")
    public ResponseEntity<ApiResponse<List<NaverPlaceResponse>>> searchNaver(
            @RequestParam String query
    ) {
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.searchNaver(query)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantResponse>> register(
            @Valid @RequestBody RestaurantRegisterRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.register(request)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> search(
            @RequestParam String keyword,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.search(keyword, pageable)));
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getDetail(
            @PathVariable Long restaurantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(restaurantService.getDetail(restaurantId)));
    }
}