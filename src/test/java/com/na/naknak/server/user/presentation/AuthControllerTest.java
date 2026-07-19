package com.na.naknak.server.user.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.naknak.server.common.auth.LoginUserArgumentResolver;
import com.na.naknak.server.common.config.WebConfig;
import com.na.naknak.server.common.config.logging.TraceIdInterceptor;
import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.user.application.AuthService;
import com.na.naknak.server.user.infrastructure.redis.BlacklistRepository;
import com.na.naknak.server.user.presentation.dto.TokenRefreshRequest;
import com.na.naknak.server.user.presentation.dto.TokenResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import({WebConfig.class, LoginUserArgumentResolver.class, TraceIdInterceptor.class})
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;
    @MockitoBean
    JwtProvider jwtProvider;
    @MockitoBean
    BlacklistRepository blacklistRepository;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 토큰_재발급_성공() throws Exception {
        given(authService.refresh("valid-rt"))
                .willReturn(new TokenResponse("new-at", "new-rt"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenRefreshRequest("valid-rt"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-at"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-rt"));
    }

    @Test
    void 토큰_재발급_refreshToken_누락_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 만료된_리프레시토큰_재발급_401() throws Exception {
        given(authService.refresh("expired-rt"))
                .willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TokenRefreshRequest("expired-rt"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 로그아웃_성공() throws Exception {
        doNothing().when(authService).logout(1L, "access-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}