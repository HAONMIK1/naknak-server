package com.na.naknak.server.user.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.user.domain.InviteCode;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.InviteCodeRepository;
import com.na.naknak.server.user.domain.repository.UserRepository;
import com.na.naknak.server.user.infrastructure.kakao.KakaoApiClient;
import com.na.naknak.server.user.infrastructure.kakao.KakaoUserInfo;
import com.na.naknak.server.user.infrastructure.redis.BlacklistRepository;
import com.na.naknak.server.user.infrastructure.redis.RefreshTokenRepository;
import com.na.naknak.server.user.presentation.dto.LoginResponse;
import com.na.naknak.server.user.presentation.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
class AuthIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private InviteCodeRepository inviteCodeRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private BlacklistRepository blacklistRepository;

    @MockBean
    private KakaoApiClient kakaoApiClient;

    private static final String KAKAO_TOKEN = "kakao-token";
    private static final String KAKAO_ID = "12345";
    private static final String INVITE_CODE = "TESTCODE";

    @BeforeEach
    void setUp() {
        User seedUser = User.create("00000", "seed@test.com", "씨앗유저");
        userRepository.save(seedUser);
        InviteCode inviteCode = InviteCode.create(INVITE_CODE, seedUser);
        inviteCodeRepository.save(inviteCode);
    }

    @Test
    void 신규_유저_로그인_NEED_SIGNUP_반환() {
        given(kakaoApiClient.getUserInfo(KAKAO_TOKEN)).willReturn(
                new KakaoUserInfo(Long.parseLong(KAKAO_ID),
                        new KakaoUserInfo.KakaoAccount("new@test.com",
                                new KakaoUserInfo.KakaoProfile("신규유저")))
        );

        LoginResponse response = userService.login(KAKAO_TOKEN);

        assertThat(response.status()).isEqualTo("NEED_SIGNUP");
        assertThat(response.kakaoId()).isEqualTo(KAKAO_ID);
    }

    @Test
    void 회원가입_성공_토큰_반환() {
        given(kakaoApiClient.getUserInfo(KAKAO_TOKEN)).willReturn(
                new KakaoUserInfo(Long.parseLong(KAKAO_ID),
                        new KakaoUserInfo.KakaoAccount("new@test.com",
                                new KakaoUserInfo.KakaoProfile("신규유저")))
        );

        LoginResponse response = userService.signup(KAKAO_TOKEN, INVITE_CODE, "신규유저");

        assertThat(response.status()).isEqualTo("AUTHENTICATED");
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
        assertThat(userRepository.findByKakaoId(KAKAO_ID)).isPresent();
    }

    @Test
    void 회원가입_후_로그인_AUTHENTICATED_반환() {
        given(kakaoApiClient.getUserInfo(KAKAO_TOKEN)).willReturn(
                new KakaoUserInfo(Long.parseLong(KAKAO_ID),
                        new KakaoUserInfo.KakaoAccount("new@test.com",
                                new KakaoUserInfo.KakaoProfile("신규유저")))
        );
        userService.signup(KAKAO_TOKEN, INVITE_CODE, "신규유저");

        LoginResponse response = userService.login(KAKAO_TOKEN);

        assertThat(response.status()).isEqualTo("AUTHENTICATED");
        assertThat(response.accessToken()).isNotNull();
    }

    @Test
    void 토큰_재발급_성공_및_RTR_검증() {
        given(kakaoApiClient.getUserInfo(KAKAO_TOKEN)).willReturn(
                new KakaoUserInfo(Long.parseLong(KAKAO_ID),
                        new KakaoUserInfo.KakaoAccount("new@test.com",
                                new KakaoUserInfo.KakaoProfile("신규유저")))
        );
        LoginResponse signup = userService.signup(KAKAO_TOKEN, INVITE_CODE, "신규유저");
        String oldRefreshToken = signup.refreshToken();

        TokenResponse response = authService.refresh(oldRefreshToken);

        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
        assertThatThrownBy(() -> authService.refresh(oldRefreshToken))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 로그아웃_후_블랙리스트_등록() {
        given(kakaoApiClient.getUserInfo(KAKAO_TOKEN)).willReturn(
                new KakaoUserInfo(Long.parseLong(KAKAO_ID),
                        new KakaoUserInfo.KakaoAccount("new@test.com",
                                new KakaoUserInfo.KakaoProfile("신규유저")))
        );
        LoginResponse signup = userService.signup(KAKAO_TOKEN, INVITE_CODE, "신규유저");
        User user = userRepository.findByKakaoId(KAKAO_ID).get();

        authService.logout(user.getId(), signup.accessToken());

        assertThat(blacklistRepository.exists(signup.accessToken())).isTrue();
        assertThat(refreshTokenRepository.find(user.getId())).isNull();
    }
}