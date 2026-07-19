package com.na.naknak.server.feed.application;

import com.na.naknak.server.follow.domain.repository.FollowRepository;
import com.na.naknak.server.follow.infrastructure.redis.NetworkDegreeCache;
import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.review.domain.Review;
import com.na.naknak.server.review.domain.repository.ReviewRepository;
import com.na.naknak.server.review.presentation.dto.ReviewResponse;
import com.na.naknak.server.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @InjectMocks
    private FeedService feedService;

    @Mock
    private FollowRepository followRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private NetworkDegreeCache networkDegreeCache;

    private final Pageable pageable = PageRequest.of(0, 20);

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

    private Review reviewByUser(Long userId, Long restaurantId) {
        return Review.create(userWithId(userId), restaurantWithId(restaurantId), "내용", 5);
    }

    private FollowRepository.NetworkDegreeRow row(Long userId, Integer degree) {
        return new FollowRepository.NetworkDegreeRow() {
            public Long getUserId() { return userId; }
            public Integer getDegree() { return degree; }
        };
    }

    @Test
    void 캐시_히트면_CTE_쿼리_없이_캐시값으로_필터링() {
        // given
        given(networkDegreeCache.find(1L)).willReturn(Optional.of(Map.of(
                1, Set.of(2L, 3L),
                2, Set.of(4L),
                3, Set.of()
        )));
        given(reviewRepository.findByUserIdInAndDeletedAtIsNull(any(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(reviewByUser(2L, 10L)), pageable, 1));

        // when
        Page<ReviewResponse> result = feedService.getFeed(1L, Set.of(1), pageable);

        // then
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(reviewRepository).findByUserIdInAndDeletedAtIsNull(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).containsExactlyInAnyOrder(2L, 3L);
        assertThat(result.getContent()).hasSize(1);
        verify(followRepository, never()).findNetworkDegrees(any(), anyInt(), anyInt());
    }


    @Test
    void 캐시_미스면_CTE로_계산_후_캐시에_저장() {
        // given
        given(networkDegreeCache.find(1L)).willReturn(Optional.empty());
        given(followRepository.findNetworkDegrees(eq(1L), eq(3), eq(5000))).willReturn(List.of(
                row(2L, 1), row(4L, 2)
        ));
        given(reviewRepository.findByUserIdInAndDeletedAtIsNull(any(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        feedService.getFeed(1L, Set.of(1, 2), pageable);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, Set<Long>>> cacheCaptor = ArgumentCaptor.forClass(Map.class);
        verify(networkDegreeCache).save(eq(1L), cacheCaptor.capture());
        assertThat(cacheCaptor.getValue().get(1)).containsExactly(2L);
        assertThat(cacheCaptor.getValue().get(2)).containsExactly(4L);

        ArgumentCaptor<Collection<Long>> reviewCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(reviewRepository).findByUserIdInAndDeletedAtIsNull(reviewCaptor.capture(), eq(pageable));
        assertThat(reviewCaptor.getValue()).containsExactlyInAnyOrder(2L, 4L);
    }

    @Test
    void 특정_촌수만_선택하면_다른_촌수는_제외() {
        // given
        given(networkDegreeCache.find(1L)).willReturn(Optional.empty());
        given(followRepository.findNetworkDegrees(eq(1L), eq(3), eq(5000))).willReturn(List.of(
                row(2L, 1), row(4L, 2), row(5L, 3)
        ));
        given(reviewRepository.findByUserIdInAndDeletedAtIsNull(any(), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        // when: 2촌만 선택
        feedService.getFeed(1L, Set.of(2), pageable);

        // then
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(reviewRepository).findByUserIdInAndDeletedAtIsNull(captor.capture(), eq(pageable));
        assertThat(captor.getValue()).containsExactly(4L);
    }

    @Test
    void degrees가_비어있으면_캐시조차_확인않고_빈_페이지_반환() {
        // when
        Page<ReviewResponse> result = feedService.getFeed(1L, Set.of(), pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        verify(networkDegreeCache, never()).find(any());
        verify(reviewRepository, never()).findByUserIdInAndDeletedAtIsNull(any(), any());
    }

    @Test
    void 팔로우가_전혀_없으면_빈_페이지_반환() {
        // given
        given(networkDegreeCache.find(1L)).willReturn(Optional.empty());
        given(followRepository.findNetworkDegrees(eq(1L), eq(3), eq(5000))).willReturn(List.of());

        // when
        Page<ReviewResponse> result = feedService.getFeed(1L, Set.of(1, 2, 3), pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        verify(reviewRepository, never()).findByUserIdInAndDeletedAtIsNull(any(), any());
    }
}
