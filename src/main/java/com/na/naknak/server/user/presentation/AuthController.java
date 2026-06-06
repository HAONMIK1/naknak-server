package com.na.naknak.server.user.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.user.application.AuthService;
import com.na.naknak.server.user.presentation.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(refreshToken)));
    }
}