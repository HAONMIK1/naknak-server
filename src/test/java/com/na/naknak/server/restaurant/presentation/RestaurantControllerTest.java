package com.na.naknak.server.restaurant.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.naknak.server.common.auth.LoginUserArgumentResolver;
import com.na.naknak.server.common.config.WebConfig;
import com.na.naknak.server.common.config.logging.TraceIdInterceptor;
import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.restaurant.application.RestaurantService;
import com.na.naknak.server.restaurant.presentation.dto.NaverPlaceResponse;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantRegisterRequest;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantResponse;
import com.na.naknak.server.user.infrastructure.redis.BlacklistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = RestaurantController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import({WebConfig.class, LoginUserArgumentResolver.class, TraceIdInterceptor.class})
class RestaurantControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RestaurantService restaurantService;
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
    void 네이버_검색_성공() throws Exception {
        given(restaurantService.searchNaver("스시"))
                .willReturn(List.of(new NaverPlaceResponse(
                        "스시로", "일식", "서울 강남구", "테헤란로 1",
                        37.5, 127.0, "http://place")));

        mockMvc.perform(get("/api/v1/restaurants/search/naver").param("query", "스시"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("스시로"));
    }

    @Test
    void 맛집_등록_성공() throws Exception {
        given(restaurantService.register(any(RestaurantRegisterRequest.class)))
                .willReturn(new RestaurantResponse(
                        10L, "스시로", "일식", "서울 강남구", 37.5, 127.0, "http://place"));

        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RestaurantRegisterRequest(
                                "스시로", "일식", "서울 강남구", 37.5, 127.0, "http://place"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("스시로"));
    }

    @Test
    void 맛집_등록_이름누락_400() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"category\":\"일식\",\"address\":\"서울\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 등록된_맛집_검색_성공() throws Exception {
        Page<RestaurantResponse> page = new PageImpl<>(List.of(
                new RestaurantResponse(10L, "스시로", "일식", "서울 강남구", 37.5, 127.0, "http://place")));
        given(restaurantService.search(eq("스시"), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/restaurants/search").param("keyword", "스시"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("스시로"));
    }

    @Test
    void 맛집_상세_조회_성공() throws Exception {
        given(restaurantService.getDetail(10L))
                .willReturn(new RestaurantResponse(
                        10L, "스시로", "일식", "서울 강남구", 37.5, 127.0, "http://place"));

        mockMvc.perform(get("/api/v1/restaurants/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void 존재하지않는_맛집_조회_404() throws Exception {
        given(restaurantService.getDetail(999L))
                .willThrow(new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/restaurants/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}