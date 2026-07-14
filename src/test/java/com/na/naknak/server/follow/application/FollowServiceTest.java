package com.na.naknak.server.follow.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.follow.domain.Follow;
import com.na.naknak.server.follow.domain.repository.FollowRepository;
import com.na.naknak.server.follow.infrastructure.redis.NetworkDegreeCache;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @InjectMocks
    private FollowService followService;

    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NetworkDegreeCache networkDegreeCache;

    private User userWithId(Long id) {
        User user = User.create(String.valueOf(id), "u" + id + "@test.com", "유저" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void 팔로우_성공() {
        // given
        given(userRepository.findById(2L)).willReturn(Optional.of(userWithId(2L)));
        given(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(false);

        // when
        followService.follow(1L, 2L);

        // then
        verify(followRepository).save(any(Follow.class));
        verify(networkDegreeCache).invalidate(1L);
    }

    @Test
    void 자기_자신_팔로우_예외() {
        // when & then
        assertThatThrownBy(() -> followService.follow(1L, 1L))
                .isInstanceOf(BusinessException.class);
        verify(followRepository, never()).save(any(Follow.class));
        verify(networkDegreeCache, never()).invalidate(any());
    }

    @Test
    void 이미_팔로우한_경우_중복저장_안함() {
        // given
        given(userRepository.findById(2L)).willReturn(Optional.of(userWithId(2L)));
        given(followRepository.existsByFollowerIdAndFollowingId(1L, 2L)).willReturn(true);

        // when
        followService.follow(1L, 2L);

        // then
        verify(followRepository, never()).save(any(Follow.class));
        verify(networkDegreeCache, never()).invalidate(any());
    }

    @Test
    void 존재하지_않는_유저_팔로우_예외() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> followService.follow(1L, 999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 언팔로우_성공() {
        // given
        Follow follow = Follow.create(1L, 2L);
        given(followRepository.findByFollowerIdAndFollowingId(1L, 2L)).willReturn(Optional.of(follow));

        // when
        followService.unfollow(1L, 2L);

        // then
        verify(followRepository).delete(follow);
        verify(networkDegreeCache).invalidate(1L);
    }

    @Test
    void 팔로잉_목록_조회() {
        // given
        given(followRepository.findByFollowerId(1L)).willReturn(
                List.of(Follow.create(1L, 2L), Follow.create(1L, 3L))
        );

        // when
        List<Long> followingIds = followService.getFollowingIds(1L);

        // then
        assertThat(followingIds).containsExactly(2L, 3L);
    }
}
