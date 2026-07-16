package com.na.naknak.server.review.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.restaurant.domain.repository.RestaurantRepository;
import com.na.naknak.server.review.domain.Review;
import com.na.naknak.server.review.domain.repository.ReviewRepository;
import com.na.naknak.server.review.presentation.dto.ReviewCreateRequest;
import com.na.naknak.server.review.presentation.dto.ReviewResponse;
import com.na.naknak.server.score.application.ScoreService;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private ScoreService scoreService;

    private User userWithId(Long id) {
        User user = User.create(String.valueOf(id), "u" + id + "@test.com", "유저" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Restaurant restaurantWithId(Long id) {
        Restaurant restaurant = Restaurant.create(null, null, "맛집" + id, "한식", "주소" + id, null, null);
        ReflectionTestUtils.setField(restaurant, "id", id);
        return restaurant;
    }

    @Test
    void 리뷰_작성_성공_이미지포함() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(userWithId(1L)));
        given(restaurantRepository.findById(2L)).willReturn(Optional.of(restaurantWithId(2L)));
        given(reviewRepository.save(any(Review.class))).willAnswer(inv -> inv.getArgument(0));
        ReviewCreateRequest request = new ReviewCreateRequest(
                "맛있어요", 5, List.of("http://img1", "http://img2"));

        // when
        ReviewResponse response = reviewService.create(1L, 2L, request);

        // then
        assertThat(response.content()).isEqualTo("맛있어요");
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.imageUrls()).containsExactly("http://img1", "http://img2");
        assertThat(response.nickname()).isEqualTo("유저1");
    }

    @Test
    void 없는_맛집에_리뷰작성_예외() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(userWithId(1L)));
        given(restaurantRepository.findById(999L)).willReturn(Optional.empty());
        ReviewCreateRequest request = new ReviewCreateRequest("내용", 3, null);

        // when & then
        assertThatThrownBy(() -> reviewService.create(1L, 999L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 본인_리뷰_수정_성공() {
        // given
        Review review = Review.create(userWithId(1L), restaurantWithId(2L), "기존 내용", 3);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));
        ReviewCreateRequest request = new ReviewCreateRequest("수정된 내용", 5, List.of("http://new-img"));

        // when
        ReviewResponse response = reviewService.update(1L, 5L, request);

        // then
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.imageUrls()).containsExactly("http://new-img");
    }

    @Test
    void 타인_리뷰_수정_FORBIDDEN() {
        // given
        Review review = Review.create(userWithId(2L), restaurantWithId(2L), "기존 내용", 3);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));
        ReviewCreateRequest request = new ReviewCreateRequest("수정된 내용", 5, null);

        // when & then
        assertThatThrownBy(() -> reviewService.update(1L, 5L, request))
                .isInstanceOf(BusinessException.class);
        assertThat(review.getContent()).isEqualTo("기존 내용");
    }

    @Test
    void 본인_리뷰_삭제_성공() {
        // given
        Review review = Review.create(userWithId(1L), restaurantWithId(2L), "내용", 4);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));

        // when
        reviewService.delete(1L, 5L);

        // then
        assertThat(review.isDeleted()).isTrue();
    }

    @Test
    void 타인_리뷰_삭제_FORBIDDEN() {
        // given
        Review review = Review.create(userWithId(2L), restaurantWithId(2L), "내용", 4);
        given(reviewRepository.findById(5L)).willReturn(Optional.of(review));

        // when & then
        assertThatThrownBy(() -> reviewService.delete(1L, 5L))
                .isInstanceOf(BusinessException.class);
        assertThat(review.isDeleted()).isFalse();
    }
}
