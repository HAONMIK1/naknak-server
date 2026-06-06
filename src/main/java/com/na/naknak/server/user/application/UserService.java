package com.na.naknak.server.user.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.common.security.JwtProvider;
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

        String newCode;
        do {
            newCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (inviteCodeRepository.existsByCode(newCode));

        inviteCodeRepository.save(InviteCode.create(newCode, user));

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken);

        return LoginResponse.authenticated(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        String inviteCode = inviteCodeRepository.findByCreatedByAndUsedByIsNull(user)
                .map(InviteCode::getCode)
                .orElse(null);
        return MyProfileResponse.from(user, inviteCode);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserProfileResponse.from(user);
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