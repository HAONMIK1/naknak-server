package com.na.naknak.server.follow.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.follow.domain.Follow;
import com.na.naknak.server.follow.domain.repository.FollowRepository;
import com.na.naknak.server.follow.infrastructure.redis.NetworkDegreeCache;
import com.na.naknak.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
