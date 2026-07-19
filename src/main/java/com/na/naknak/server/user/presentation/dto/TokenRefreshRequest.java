package com.na.naknak.server.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(@NotBlank String refreshToken) {}