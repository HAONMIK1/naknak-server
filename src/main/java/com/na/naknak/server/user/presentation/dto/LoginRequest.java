package com.na.naknak.server.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String authCode
) {}
