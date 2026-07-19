package com.na.naknak.server.user.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.common.auth.LoginUser;
import com.na.naknak.server.user.application.AuthService;
import com.na.naknak.server.user.presentation.dto.TokenRefreshRequest;
import com.na.naknak.server.user.presentation.dto.TokenResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestBody @Valid TokenRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @LoginUser Long userId,
            @RequestHeader("Authorization") String authHeader
    ) {
        authService.logout(userId, authHeader.substring(7));
        return ResponseEntity.ok(ApiResponse.ok("로그아웃 완료"));
    }
}