package com.na.naknak.server.user.presentation.dto;

public record LoginResponse(
        String status,
        String accessToken,
        String refreshToken,
        String kakaoId,
        String email,
        String nickname,
        String kakaoAccessToken
) {
    public static LoginResponse authenticated(String accessToken, String refreshToken) {
        return new LoginResponse("AUTHENTICATED", accessToken, refreshToken, null, null, null, null);
    }

    public static LoginResponse needSignup(String kakaoId, String email, String nickname, String kakaoAccessToken) {
        return new LoginResponse("NEED_SIGNUP", null, null, kakaoId, email, nickname, kakaoAccessToken);
    }
}
