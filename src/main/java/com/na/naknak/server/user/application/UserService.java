package com.na.naknak.server.user.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.follow.application.FollowService;
import com.na.naknak.server.follow.domain.repository.FollowRepository;
import com.na.naknak.server.score.application.ScoreService;
import com.na.naknak.server.score.domain.ScoreReason;
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
import com.na.naknak.server.user.presentation.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KakaoApiClient kakaoApiClient;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final BlacklistRepository blacklistRepository;
    private final FollowRepository followRepository;
    private final FollowService followService;
    private final ScoreService scoreService;

    @Transactional
    public LoginResponse login(String authCode) {
        String kakaoAccessToken = kakaoApiClient.getAccessToken(authCode);
        KakaoUserInfo kakaoUser = kakaoApiClient.getUserInfo(kakaoAccessToken);
        String kakaoId = String.valueOf(kakaoUser.id());

        return userRepository.findByKakaoId(kakaoId)
                .map(user -> {
                    String accessToken = jwtProvider.createAccessToken(user.getId());
                    String refreshToken = jwtProvider.createRefreshToken(user.getId());
                    refreshTokenRepository.save(user.getId(), refreshToken);
                    return LoginResponse.authenticated(accessToken, refreshToken);
                })
                .orElseGet(() -> LoginResponse.needSignup(
                        kakaoId,
                        kakaoUser.email(),
                        kakaoUser.nickname(),
                        kakaoAccessToken
                ));
    }

    @Transactional
    public LoginResponse signup(String kakaoAccessToken, String inviteCode, String nickname) {
        KakaoUserInfo kakaoUser = kakaoApiClient.getUserInfo(kakaoAccessToken);
        String kakaoId = String.valueOf(kakaoUser.id());

        InviteCode invite = inviteCodeRepository.findByCodeAndUsedByIsNull(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));

        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.create(kakaoId, kakaoUser.email(), nickname);
        userRepository.save(user);
        invite.use(user);

        Long inviterId = invite.getCreatedBy().getId();
        followService.follow(inviterId, user.getId());
        followService.follow(user.getId(), inviterId);
        scoreService.earn(inviterId, ScoreReason.INVITE);

        inviteCodeRepository.save(InviteCode.create(generateUniqueInviteCode(), user));

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken);

        return LoginResponse.authenticated(accessToken, refreshToken);
    }

    @Transactional
    public MyProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String inviteCode = getOrCreateActiveInviteCode(user);
        long followerCount = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        return MyProfileResponse.from(user, inviteCode, followerCount, followingCount);
    }

    /** 미사용 초대코드가 있으면 그대로, 없으면(이미 다 써서 소진됐으면) 새로 발급한다. */
    private String getOrCreateActiveInviteCode(User user) {
        return inviteCodeRepository.findByCreatedByAndUsedByIsNull(user)
                .map(InviteCode::getCode)
                .orElseGet(() -> {
                    String newCode = generateUniqueInviteCode();
                    inviteCodeRepository.save(InviteCode.create(newCode, user));
                    return newCode;
                });
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (inviteCodeRepository.existsByCode(code));
        return code;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long currentUserId, Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        long followerCount = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, userId);
        return UserProfileResponse.from(user, followerCount, followingCount, isFollowing);
    }

    @Transactional
    public void updateNickname(Long userId, String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.updateNickname(nickname);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> searchUsers(String keyword) {
        return userRepository.findByNicknameContaining(keyword).stream()
                .filter(u -> !u.isDeleted())
                .map(UserProfileResponse::from)
                .toList();
    }

    @Transactional
    public void withdraw(Long userId, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.delete();
        refreshTokenRepository.delete(userId);
        long ttl = jwtProvider.getExpiration(accessToken);
        if (ttl > 0) {
            blacklistRepository.save(accessToken, ttl);
        }
    }


}