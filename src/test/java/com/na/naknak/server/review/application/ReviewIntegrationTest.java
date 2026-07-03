package com.na.naknak.server.review.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.restaurant.domain.repository.RestaurantRepository;
import com.na.naknak.server.review.domain.repository.ReviewRepository;
import com.na.naknak.server.review.presentation.dto.ReviewCreateRequest;
import com.na.naknak.server.review.presentation.dto.ReviewResponse;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
class ReviewIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RestaurantRepository restaurantRepository;

    private Long userId;
    private Long otherUserId;
    private Long restaurantId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.create("10001", "author@test.com", "작성자"));
        User other = userRepository.save(User.create("10002", "other@test.com", "타인"));
        Restaurant restaurant = restaurantRepository.save(Restaurant.create(
                null, null, "스시로", "일식", "서울 강남구", 37.5, 127.0));
        userId = user.getId();
        otherUserId = other.getId();
        restaurantId = restaurant.getId();
    }

    @Test
    void 리뷰_작성_DB저장_이미지포함() {
        ReviewCreateRequest request = new ReviewCreateRequest(
                "맛있어요", 5, List.of("http://img1", "http://img2"));

        ReviewResponse response = reviewService.create(userId, restaurantId, request);

        assertThat(response.id()).isNotNull();
        assertThat(response.imageUrls()).containsExactly("http://img1", "http://img2");
        assertThat(reviewRepository.findById(response.id())).isPresent();
    }

    @Test
    void 삭제된_리뷰는_맛집_목록에서_제외() {
        ReviewResponse created = reviewService.create(
                userId, restaurantId, new ReviewCreateRequest("맛있어요", 5, null));

        Page<ReviewResponse> before = reviewService.getRestaurantReviews(
                restaurantId, PageRequest.of(0, 10));
        assertThat(before.getContent()).hasSize(1);

        reviewService.delete(userId, created.id());

        Page<ReviewResponse> after = reviewService.getRestaurantReviews(
                restaurantId, PageRequest.of(0, 10));
        assertThat(after.getContent()).isEmpty();
    }

    @Test
    void 내_리뷰_목록_조회() {
        reviewService.create(userId, restaurantId, new ReviewCreateRequest("첫 리뷰", 4, null));
        reviewService.create(userId, restaurantId, new ReviewCreateRequest("둘째 리뷰", 5, null));

        Page<ReviewResponse> result = reviewService.getMyReviews(userId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void 타인_리뷰_삭제_FORBIDDEN() {
        ReviewResponse created = reviewService.create(
                userId, restaurantId, new ReviewCreateRequest("맛있어요", 5, null));

        assertThatThrownBy(() -> reviewService.delete(otherUserId, created.id()))
                .isInstanceOf(BusinessException.class);
        assertThat(reviewRepository.findById(created.id())).get()
                .extracting(r -> r.isDeleted()).isEqualTo(false);
    }
}
