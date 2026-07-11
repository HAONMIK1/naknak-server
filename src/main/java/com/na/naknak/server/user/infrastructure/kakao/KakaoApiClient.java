package com.na.naknak.server.user.infrastructure.kakao;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class KakaoApiClient {

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private final RestClient restClient;

    public KakaoApiClient() {
        this.restClient = RestClient.create();
    }

    public String getAccessToken(String authCode) {
        String body = "grant_type=authorization_code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&code=" + authCode +
                "&client_secret=" + clientSecret;

        KakaoTokenResponse response = restClient.post()
                .uri(KAKAO_TOKEN_URL)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String errorBody = new String(res.getBody().readAllBytes());
                    log.warn("카카오 토큰 발급 실패: status={}, body={}", res.getStatusCode(), errorBody);
                    throw new BusinessException(ErrorCode.KAKAO_API_ERROR);
                })
                .body(KakaoTokenResponse.class);

        return response.accessToken();
    }

    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        return restClient.get()
                .uri(KAKAO_USER_INFO_URL)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new BusinessException(ErrorCode.KAKAO_API_ERROR);
                })
                .body(KakaoUserInfo.class);
    }
}
