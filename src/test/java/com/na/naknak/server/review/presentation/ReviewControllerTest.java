package com.na.naknak.server.review.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.naknak.server.common.auth.LoginUserArgumentResolver;
import com.na.naknak.server.common.config.WebConfig;
import com.na.naknak.server.common.config.logging.TraceIdInterceptor;
import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.common.security.JwtProvider;
import com.na.naknak.server.review.application.ReviewService;
import com.na.naknak.server.review.presentation.dto.ReviewCreateRequest;
import com.na.naknak.server.review.presentation.dto.ReviewResponse;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ReviewController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@Import({WebConfig.class, LoginUserArgumentResolver.class, TraceIdInterceptor.class})
class ReviewControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    ReviewService reviewService;
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

    private ReviewResponse sampleResponse() {
        return new ReviewResponse(
                100L, 10L, "스시로", 1L, "낙낙유저", "맛있어요", 5,
                List.of("http://img1"), LocalDateTime.now());
    }

    @Test
    void 리뷰_작성_성공() throws Exception {
        given(reviewService.create(eq(1L), eq(10L), any(ReviewCreateRequest.class)))
                .willReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/restaurants/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReviewCreateRequest(
                                "맛있어요", 5, List.of("http://img1")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.imageUrls.length()").value(1));
    }

    @Test
    void 리뷰_작성_내용누락_400() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\",\"rating\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 리뷰_작성_별점범위초과_400() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants/10/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"좋아요\",\"rating\":6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 맛집_리뷰목록_조회_성공() throws Exception {
        Page<ReviewResponse> page = new PageImpl<>(List.of(sampleResponse()));
        given(reviewService.getRestaurantReviews(eq(10L), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/restaurants/10/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].nickname").value("낙낙유저"));
    }

    @Test
    void 내_리뷰목록_조회_성공() throws Exception {
        Page<ReviewResponse> page = new PageImpl<>(List.of(sampleResponse()));
        given(reviewService.getMyReviews(eq(1L), any(Pageable.class)))
                .willReturn(page);

        mockMvc.perform(get("/api/v1/users/me/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void 본인_리뷰_삭제_성공() throws Exception {
        doNothing().when(reviewService).delete(1L, 100L);

        mockMvc.perform(delete("/api/v1/reviews/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 타인_리뷰_삭제_403() throws Exception {
        doThrow(new BusinessException(ErrorCode.FORBIDDEN))
                .when(reviewService).delete(1L, 100L);

        mockMvc.perform(delete("/api/v1/reviews/100"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
