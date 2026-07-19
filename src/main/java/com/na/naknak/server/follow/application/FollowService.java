package com.na.naknak.server.follow.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.follow.domain.Follow;
import com.na.naknak.server.follow.domain.repository.FollowRepository;
import com.na.naknak.server.follow.infrastructure.redis.NetworkDegreeCache;
import com.na.naknak.server.follow.presentation.dto.FollowUserResponse;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NetworkDegreeCache networkDegreeCache;

    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(ErrorCode.SELF_FOLLOW_NOT_ALLOWED);
        }
        userRepository.findById(followingId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            return;
        }
        followRepository.save(Follow.create(followerId, followingId));
        networkDegreeCache.invalidate(followerId);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .ifPresent(follow -> {
                    followRepository.delete(follow);
                    networkDegreeCache.invalidate(followerId);
                });
    }

    @Transactional(readOnly = true)
    public List<Long> getFollowingIds(Long followerId) {
        return followRepository.findByFollowerId(followerId).stream()
                .map(Follow::getFollowingId)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowers(Long userId) {
        List<Long> followerIds = followRepository.findByFollowingId(userId).stream()
                .map(Follow::getFollowerId)
                .toList();
        return toFollowUserResponses(followerIds);
    }

    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowingUsers(Long userId) {
        List<Long> followingIds = followRepository.findByFollowerId(userId).stream()
                .map(Follow::getFollowingId)
                .toList();
        return toFollowUserResponses(followingIds);
    }

    private List<FollowUserResponse> toFollowUserResponses(List<Long> userIds) {
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return userIds.stream()
                .map(usersById::get)
                .filter(Objects::nonNull)
                .map(FollowUserResponse::from)
                .toList();
    }
}
