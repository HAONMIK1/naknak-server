package com.na.naknak.server.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank String kakaoAccessToken,
        @NotBlank String inviteCode,
        @NotBlank String nickname
) {}