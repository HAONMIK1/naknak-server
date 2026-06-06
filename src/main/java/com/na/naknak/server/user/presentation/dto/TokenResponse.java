package com.na.naknak.server.user.presentation.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}