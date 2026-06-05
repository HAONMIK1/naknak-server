package com.na.naknak.server.user.application;

import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import com.na.naknak.server.user.infrastructure.kakao.KakaoApiClient;
import com.na.naknak.server.user.infrastructure.kakao.KakaoUserInfo;
import com.na.naknak.server.user.infrastructure.redis.RefreshTokenRepository;
import com.na.naknak.server.user.presentation.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private KakaoApiClient kakaoApiClient;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Test
    void 기존_유저_로그인_AUTHENTICATED_반환() {
        // given
        KakaoUserInfo kakaoUserInfo = new KakaoUserInfo(
                12345L,
                new KakaoUserInfo.KakaoAccount("test@test.com",
                        new KakaoUserInfo.KakaoProfile("테스트유저"))
        );
        User user = User.create("12345", "test@test.com", "테스트유저");

        given(kakaoApiClient.getUserInfo("kakao-token")).willReturn(kakaoUserInfo);
        given(userRepository.findByKakaoId("12345")).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(user.getId())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(user.getId())).willReturn("refresh-token");

        // when
        LoginResponse response = userService.login("kakao-token");

        // then
        assertThat(response.status()).isEqualTo("AUTHENTICATED");
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(user.getId(), "refresh-token");
    }

    @Test
    void 신규_유저_로그인_NEED_SIGNUP_반환() {
        // given
        KakaoUserInfo kakaoUserInfo = new KakaoUserInfo(
                99999L,
                new KakaoUserInfo.KakaoAccount("new@test.com",
                        new KakaoUserInfo.KakaoProfile("신규유저"))
        );

        given(kakaoApiClient.getUserInfo("kakao-token")).willReturn(kakaoUserInfo);
        given(userRepository.findByKakaoId("99999")).willReturn(Optional.empty());

        // when
        LoginResponse response = userService.login("kakao-token");

        // then
        assertThat(response.status()).isEqualTo("NEED_SIGNUP");
        assertThat(response.kakaoId()).isEqualTo("99999");
        assertThat(response.email()).isEqualTo("new@test.com");
        assertThat(response.nickname()).isEqualTo("신규유저");
    }

    @Test
    void 유효하지_않은_초대코드_회원가입_예외() {}

    @Test
    void 닉네임_중복_회원가입_예외() {}

    @Test
    void 정상_회원가입_토큰_반환() {}
}