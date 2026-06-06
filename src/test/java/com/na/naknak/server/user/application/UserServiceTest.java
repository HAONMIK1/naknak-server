package com.na.naknak.server.user.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.user.domain.InviteCode;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.InviteCodeRepository;
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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private KakaoApiClient kakaoApiClient;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private InviteCodeRepository inviteCodeRepository;

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
    void 유효하지_않은_초대코드_회원가입_예외() {
        // given
        given(kakaoApiClient.getUserInfo("kakao-token")).willReturn(
                new KakaoUserInfo(12345L, new KakaoUserInfo.KakaoAccount("a@a.com",
                        new KakaoUserInfo.KakaoProfile("닉네임")))
        );
        given(inviteCodeRepository.findByCodeAndUsedByIsNull("INVALID")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.signup("kakao-token", "INVALID", "닉네임"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 닉네임_중복_회원가입_예외() {
        // given
        InviteCode invite = InviteCode.create("ABC12345", User.create("0", "c@c.com", "초대자"));
        given(kakaoApiClient.getUserInfo("kakao-token")).willReturn(
                new KakaoUserInfo(12345L, new KakaoUserInfo.KakaoAccount("a@a.com",
                        new KakaoUserInfo.KakaoProfile("중복닉네임")))
        );
        given(inviteCodeRepository.findByCodeAndUsedByIsNull("ABC12345")).willReturn(Optional.of(invite));
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.signup("kakao-token", "ABC12345", "중복닉네임"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 정상_회원가입_토큰_반환() {
        // given
        InviteCode invite = InviteCode.create("ABC12345", User.create("0", "c@c.com", "초대자"));
        given(kakaoApiClient.getUserInfo("kakao-token")).willReturn(
                new KakaoUserInfo(12345L, new KakaoUserInfo.KakaoAccount("a@a.com",
                        new KakaoUserInfo.KakaoProfile("신규닉네임")))
        );
        given(inviteCodeRepository.findByCodeAndUsedByIsNull("ABC12345")).willReturn(Optional.of(invite));
        given(userRepository.existsByNickname("신규닉네임")).willReturn(false);
        given(jwtProvider.createAccessToken(any())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(any())).willReturn("refresh-token");

        // when
        LoginResponse response = userService.signup("kakao-token", "ABC12345", "신규닉네임");

        // then
        assertThat(response.status()).isEqualTo("AUTHENTICATED");
        assertThat(response.accessToken()).isEqualTo("access-token");
    }

}