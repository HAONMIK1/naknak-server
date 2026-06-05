package com.na.naknak.server.user.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.user.infrastructure.redis.RefreshTokenRepository;
import com.na.naknak.server.user.presentation.dto.TokenResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Test
    void 유효한_리프레시_토큰_재발급_성공() {
        // given
        given(jwtProvider.validate("valid-rt")).willReturn(true);
        given(jwtProvider.getUserId("valid-rt")).willReturn(1L);
        given(refreshTokenRepository.find(1L)).willReturn("valid-rt");
        given(jwtProvider.createAccessToken(1L)).willReturn("new-at");
        given(jwtProvider.createRefreshToken(1L)).willReturn("new-rt");

        // when
        TokenResponse response = authService.refresh("valid-rt");

        // then
        assertThat(response.accessToken()).isEqualTo("new-at");
        assertThat(response.refreshToken()).isEqualTo("new-rt");
        verify(refreshTokenRepository).delete(1L);
        verify(refreshTokenRepository).save(1L, "new-rt");
    }

    @Test
    void Redis와_불일치한_리프레시_토큰_재발급_예외() {
        // given
        given(jwtProvider.validate("stolen-rt")).willReturn(true);
        given(jwtProvider.getUserId("stolen-rt")).willReturn(1L);
        given(refreshTokenRepository.find(1L)).willReturn("original-rt");

        // when & then
        assertThatThrownBy(() -> authService.refresh("stolen-rt"))
                .isInstanceOf(BusinessException.class);
        verify(refreshTokenRepository).delete(1L);
    }
}