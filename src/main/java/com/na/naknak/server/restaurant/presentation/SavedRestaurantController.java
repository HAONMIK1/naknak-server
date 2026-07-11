package com.na.naknak.server.restaurant.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.common.auth.LoginUser;
import com.na.naknak.server.restaurant.application.SavedRestaurantService;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SavedRestaurantController {

    private final SavedRestaurantService savedRestaurantService;

    @PostMapping("/api/v1/restaurants/{restaurantId}/save")
    public ResponseEntity<ApiResponse<Void>> save(
            @LoginUser Long userId,
            @PathVariable Long restaurantId
    ) {
        savedRestaurantService.save(userId, restaurantId);
        return ResponseEntity.ok(ApiResponse.ok("맛집을 저장했습니다."));
    }

    @DeleteMapping("/api/v1/restaurants/{restaurantId}/save")
    public ResponseEntity<ApiResponse<Void>> unsave(
            @LoginUser Long userId,
            @PathVariable Long restaurantId
    ) {
        savedRestaurantService.unsave(userId, restaurantId);
        return ResponseEntity.ok(ApiResponse.ok("저장을 취소했습니다."));
    }

    @GetMapping("/api/v1/users/me/saved-restaurants")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> getMySaved(
            @LoginUser Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(savedRestaurantService.getSaved(userId, pageable)));
    }

    @GetMapping("/api/v1/users/{userId}/saved-restaurants")
    public ResponseEntity<ApiResponse<Page<RestaurantResponse>>> getUserSaved(
            @PathVariable Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(savedRestaurantService.getSaved(userId, pageable)));
    }
}
