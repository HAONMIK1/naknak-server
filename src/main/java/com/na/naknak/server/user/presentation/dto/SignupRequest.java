package com.na.naknak.server.user.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String kakaoAccessToken,
        @NotBlank String inviteCode,
        @NotBlank
        @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "닉네임은 한글, 영문, 숫자만 사용 가능합니다.")
        String nickname
) {}