package com.na.naknak.server.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameUpdateRequest(
        @NotBlank @Size(min = 2, max = 30) String nickname
) {}