package com.na.naknak.server.user.infrastructure.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfo(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public record KakaoAccount(
            String email,
            KakaoProfile profile
    ) {}

    public record KakaoProfile(String nickname) {}

    public String email() {
        return kakaoAccount != null ? kakaoAccount.email() : null;
    }

    public String nickname() {
        return kakaoAccount != null && kakaoAccount.profile() != null
                ? kakaoAccount.profile().nickname() : null;
    }
}