package com.na.naknak.server.user.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.user.application.UserService;
import com.na.naknak.server.user.presentation.dto.LoginRequest;
import com.na.naknak.server.user.presentation.dto.LoginResponse;
import com.na.naknak.server.user.presentation.dto.SignupRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.na.naknak.server.common.auth.LoginUser;
import com.na.naknak.server.user.presentation.dto.MyProfileResponse;
import com.na.naknak.server.user.presentation.dto.NicknameUpdateRequest;
import com.na.naknak.server.user.presentation.dto.UserProfileResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request.authCode());
        HttpStatus status = "NEED_SIGNUP".equals(response.status()) ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.ok(response));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<LoginResponse>> signup(@Valid @RequestBody SignupRequest request) {
        LoginResponse response = userService.signup(
                request.kakaoAccessToken(),
                request.inviteCode(),
                request.nickname()
        );
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(@LoginUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getMyProfile(userId)));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @LoginUser Long currentUserId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserProfile(currentUserId, userId)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @LoginUser Long userId,
            @Valid @RequestBody NicknameUpdateRequest request
    ) {
        userService.updateNickname(userId, request.nickname());
        return ResponseEntity.ok(ApiResponse.ok("닉네임이 수정되었습니다."));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> searchUsers(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.searchUsers(keyword)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @LoginUser Long userId,
            @RequestHeader("Authorization") String authHeader
    ) {
        userService.withdraw(userId, authHeader.substring(7));
        return ResponseEntity.ok(ApiResponse.ok("회원탈퇴가 완료되었습니다."));
    }

}