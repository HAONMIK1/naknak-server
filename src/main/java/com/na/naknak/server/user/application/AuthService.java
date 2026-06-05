package com.na.naknak.server.user.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.user.infrastructure.redis.RefreshTokenRepository;
import com.na.naknak.server.user.presentation.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenResponse refresh(String refreshToken) {
        if (!jwtProvider.validate(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        String stored = refreshTokenRepository.find(userId);

        if (!refreshToken.equals(stored)) {
            refreshTokenRepository.delete(userId);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        refreshTokenRepository.delete(userId);
        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);
        refreshTokenRepository.save(userId, newRefreshToken);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }
}