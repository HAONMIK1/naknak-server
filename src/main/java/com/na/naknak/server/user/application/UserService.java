package com.na.naknak.server.user.application;

import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.user.domain.repository.InviteCodeRepository;
import com.na.naknak.server.user.domain.repository.UserRepository;
import com.na.naknak.server.user.infrastructure.kakao.KakaoApiClient;
import com.na.naknak.server.user.infrastructure.kakao.KakaoUserInfo;
import com.na.naknak.server.user.infrastructure.redis.RefreshTokenRepository;
import com.na.naknak.server.user.presentation.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final KakaoApiClient kakaoApiClient;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final InviteCodeRepository inviteCodeRepository;

    @Transactional
    public LoginResponse login(String kakaoAccessToken) {
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
                        kakaoUser.nickname()
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

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken);

        return LoginResponse.authenticated(accessToken, refreshToken);
    }
}