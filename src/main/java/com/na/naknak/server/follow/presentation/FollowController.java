package com.na.naknak.server.follow.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.common.auth.LoginUser;
import com.na.naknak.server.follow.application.FollowService;
import com.na.naknak.server.follow.presentation.dto.FollowUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/api/v1/users/{userId}/follow")
    public ResponseEntity<ApiResponse<Void>> follow(
            @LoginUser Long currentUserId,
            @PathVariable Long userId
    ) {
        followService.follow(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.ok("팔로우했습니다."));
    }

    @DeleteMapping("/api/v1/users/{userId}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollow(
            @LoginUser Long currentUserId,
            @PathVariable Long userId
    ) {
        followService.unfollow(currentUserId, userId);
        return ResponseEntity.ok(ApiResponse.ok("언팔로우했습니다."));
    }

    @GetMapping("/api/v1/users/me/following")
    public ResponseEntity<ApiResponse<List<Long>>> getFollowingIds(@LoginUser Long currentUserId) {
        return ResponseEntity.ok(ApiResponse.ok(followService.getFollowingIds(currentUserId)));
    }

    @GetMapping("/api/v1/users/me/followers")
    public ResponseEntity<ApiResponse<List<FollowUserResponse>>> getFollowers(@LoginUser Long currentUserId) {
        return ResponseEntity.ok(ApiResponse.ok(followService.getFollowers(currentUserId)));
    }

    @GetMapping("/api/v1/users/me/following-users")
    public ResponseEntity<ApiResponse<List<FollowUserResponse>>> getFollowingUsers(@LoginUser Long currentUserId) {
        return ResponseEntity.ok(ApiResponse.ok(followService.getFollowingUsers(currentUserId)));
    }
}
