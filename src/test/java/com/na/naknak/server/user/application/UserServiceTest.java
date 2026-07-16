package com.na.naknak.server.user.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.follow.application.FollowService;
import com.na.naknak.server.follow.domain.repository.FollowRepository;
import com.na.naknak.server.score.application.ScoreService;
import com.na.naknak.server.user.domain.InviteCode;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.InviteCodeRepository;
import com.na.naknak.server.user.domain.repository.UserRepository;
import com.na.naknak.server.user.infrastructure.kakao.KakaoApiClient;
import com.na.naknak.server.user.infrastructure.kakao.KakaoUserInfo;
import com.na.naknak.server.user.infrastructure.redis.BlacklistRepository;
import com.na.naknak.server.user.infrastructure.redis.RefreshTokenRepository;
import com.na.naknak.server.user.presentation.dto.LoginResponse;
import com.na.naknak.server.user.presentation.dto.MyProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.na.naknak.server.user.infrastructure.redis.BlacklistRepository;
import com.na.naknak.server.user.presentation.dto.UserProfileResponse;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
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
    @Mock
    private BlacklistRepository blacklistRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private FollowService followService;
    @Mock
    private ScoreService scoreService;

    @Test
    void 기존_유저_로그인_AUTHENTICATED_반환() {
        // given
        KakaoUserInfo kakaoUserInfo = new KakaoUserInfo(
                12345L,
                new KakaoUserInfo.KakaoAccount("test@test.com",
                        new KakaoUserInfo.KakaoProfile("테스트유저"))
        );
        User user = User.create("12345", "test@test.com", "테스트유저");

        given(kakaoApiClient.getAccessToken("auth-code")).willReturn("kakao-token");
        given(kakaoApiClient.getUserInfo("kakao-token")).willReturn(kakaoUserInfo);
        given(userRepository.findByKakaoId("12345")).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(user.getId())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(user.getId())).willReturn("refresh-token");

        // when
        LoginResponse response = userService.login("auth-code");

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

        given(kakaoApiClient.getAccessToken("auth-code")).willReturn("kakao-token");
        given(kakaoApiClient.getUserInfo("kakao-token")).willReturn(kakaoUserInfo);
        given(userRepository.findByKakaoId("99999")).willReturn(Optional.empty());

        // when
        LoginResponse response = userService.login("auth-code");

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

    @Test
    void 회원가입시_초대자와_자동으로_상호_팔로우된다() {
        // given
        User inviter = User.create("0", "c@c.com", "초대자");
        ReflectionTestUtils.setField(inviter, "id", 1L);
        InviteCode invite = InviteCode.create("ABC12345", inviter);

        given(kakaoApiClient.getUserInfo("kakao-token")).willReturn(
                new KakaoUserInfo(12345L, new KakaoUserInfo.KakaoAccount("a@a.com",
                        new KakaoUserInfo.KakaoProfile("신규닉네임")))
        );
        given(inviteCodeRepository.findByCodeAndUsedByIsNull("ABC12345")).willReturn(Optional.of(invite));
        given(userRepository.existsByNickname("신규닉네임")).willReturn(false);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });
        given(jwtProvider.createAccessToken(any())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(any())).willReturn("refresh-token");

        // when
        userService.signup("kakao-token", "ABC12345", "신규닉네임");

        // then
        verify(followService).follow(1L, 100L);
        verify(followService).follow(100L, 1L);
    }

    @Test
    void 내_프로필_조회_성공() {
        // given
        User user = User.create("12345", "test@test.com", "테스트유저");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(followRepository.countByFollowingId(1L)).willReturn(3L);
        given(followRepository.countByFollowerId(1L)).willReturn(5L);

        // when
        MyProfileResponse response = userService.getMyProfile(1L);

        // then
        assertThat(response.nickname()).isEqualTo("테스트유저");
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.followerCount()).isEqualTo(3L);
        assertThat(response.followingCount()).isEqualTo(5L);
    }

    @Test
    void 존재하지_않는_유저_조회_시_예외() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMyProfile(999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 타인_프로필_조회_성공() {
        // given
        User user = User.create("99999", "other@test.com", "타인유저");
        given(userRepository.findById(2L)).willReturn(Optional.of(user));
        given(followRepository.countByFollowingId(2L)).willReturn(7L);
        given(followRepository.countByFollowerId(2L)).willReturn(1L);
        given(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(true);

        // when
        UserProfileResponse response = userService.getUserProfile(1L, 2L);

        // then
        assertThat(response.nickname()).isEqualTo("타인유저");
        assertThat(response.followerCount()).isEqualTo(7L);
        assertThat(response.followingCount()).isEqualTo(1L);
        assertThat(response.isFollowing()).isTrue();
    }

    @Test
    void 닉네임_수정_성공() {
        // given
        User user = User.create("12345", "test@test.com", "기존닉네임");
        given(userRepository.existsByNickname("새닉네임")).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        userService.updateNickname(1L, "새닉네임");

        // then
        assertThat(user.getNickname()).isEqualTo("새닉네임");
        verify(userRepository).save(user);
    }

    @Test
    void 중복_닉네임으로_수정_시_예외() {
        // given
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(1L, "중복닉네임"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 유저_검색_성공() {
        // given
        List<User> users = List.of(
                User.create("1", "a@test.com", "낙낙유저1"),
                User.create("2", "b@test.com", "낙낙유저2")
        );
        given(userRepository.findByNicknameContaining("낙낙")).willReturn(users);

        // when
        List<UserProfileResponse> result = userService.searchUsers("낙낙");

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).nickname()).isEqualTo("낙낙유저1");
    }

    @Test
    void 회원탈퇴_성공() {
        // given
        User user = User.create("12345", "test@test.com", "테스트유저");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtProvider.getExpiration("access-token")).willReturn(900L);

        // when
        userService.withdraw(1L, "access-token");

        // then
        assertThat(user.isDeleted()).isTrue();
        verify(refreshTokenRepository).delete(1L);
        verify(blacklistRepository).save("access-token", 900L);
    }
}