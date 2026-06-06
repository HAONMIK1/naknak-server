package com.na.naknak.server.user.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.naknak.server.common.auth.LoginUserArgumentResolver;
import com.na.naknak.server.common.config.WebConfig;
import com.na.naknak.server.common.config.logging.TraceIdInterceptor;
import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.user.application.UserService;
import com.na.naknak.server.user.infrastructure.redis.BlacklistRepository;
import com.na.naknak.server.user.presentation.dto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import({WebConfig.class, LoginUserArgumentResolver.class, TraceIdInterceptor.class})
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    UserService userService;
    @MockBean
    JwtProvider jwtProvider;
    @MockBean
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
    void 로그인_기존유저_200() throws Exception {
        given(userService.login("kakao-token"))
                .willReturn(LoginResponse.authenticated("at", "rt"));

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("kakao-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.data.accessToken").value("at"));
    }

    @Test
    void 로그인_신규유저_201() throws Exception {
        given(userService.login("kakao-token"))
                .willReturn(LoginResponse.needSignup("12345", "test@test.com", "테스터"));

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("kakao-token"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("NEED_SIGNUP"))
                .andExpect(jsonPath("$.data.kakaoId").value("12345"));
    }

    @Test
    void 로그인_토큰누락_400() throws Exception {
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kakaoAccessToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 회원가입_성공_200() throws Exception {
        given(userService.signup("kakao-token", "ABCD1234", "낙낙유저"))
                .willReturn(LoginResponse.authenticated("at", "rt"));

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest("kakao-token", "ABCD1234", "낙낙유저"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AUTHENTICATED"));
    }

    @Test
    void 회원가입_초대코드누락_400() throws Exception {
        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kakaoAccessToken\":\"t\",\"inviteCode\":\"\",\"nickname\":\"낙낙\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 내_프로필_조회_성공() throws Exception {
        given(userService.getMyProfile(1L))
                .willReturn(new MyProfileResponse(1L, "낙낙유저", "test@test.com", "ABCD1234"));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("낙낙유저"))
                .andExpect(jsonPath("$.data.inviteCode").value("ABCD1234"));
    }

    @Test
    void 타인_프로필_조회_성공() throws Exception {
        given(userService.getUserProfile(2L))
                .willReturn(new UserProfileResponse(2L, "타인유저", "other@test.com"));

        mockMvc.perform(get("/api/v1/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("타인유저"));
    }

    @Test
    void 존재하지않는_유저_조회_404() throws Exception {
        given(userService.getUserProfile(999L))
                .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 닉네임_수정_성공() throws Exception {
        doNothing().when(userService).updateNickname(1L, "새닉네임");

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NicknameUpdateRequest("새닉네임"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 닉네임_1자_400() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"A\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 유저_검색_성공() throws Exception {
        given(userService.searchUsers("낙낙"))
                .willReturn(List.of(
                        new UserProfileResponse(1L, "낙낙유저1", "a@test.com"),
                        new UserProfileResponse(2L, "낙낙유저2", "b@test.com")
                ));

        mockMvc.perform(get("/api/v1/users/search").param("keyword", "낙낙"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void 회원탈퇴_성공() throws Exception {
        doNothing().when(userService).withdraw(1L, "access-token");

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}