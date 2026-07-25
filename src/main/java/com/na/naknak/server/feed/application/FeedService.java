package com.na.naknak.server.feed.application;

import com.na.naknak.server.follow.domain.repository.FollowRepository;
import com.na.naknak.server.follow.infrastructure.redis.NetworkDegreeCache;
import com.na.naknak.server.review.domain.repository.ReviewRepository;
import com.na.naknak.server.review.presentation.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private static final int MAX_DEGREE = 3;
    // 100만 유저 규모에서 촌수 계산 결과가 과도하게 커지는 것을 막는 안전장치.
    private static final int MAX_NETWORK_SIZE = 5000;

    private final FollowRepository followRepository;
    private final ReviewRepository reviewRepository;
    private final NetworkDegreeCache networkDegreeCache;

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getFeed(Long userId, Set<Integer> degrees, Pageable pageable) {
        if (degrees == null || degrees.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Map<Integer, Set<Long>> degreeGroups = networkDegreeCache.find(userId)
                .orElseGet(() -> {
                    Map<Integer, Set<Long>> computed = calculateDegrees(userId);
                    networkDegreeCache.save(userId, computed);
                    return computed;
                });

        Set<Long> targetUserIds = degrees.stream()
                .flatMap(degree -> degreeGroups.getOrDefault(degree, Set.of()).stream())
                .collect(Collectors.toSet());

        if (targetUserIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        return reviewRepository.findByUserIdInAndDeletedAtIsNull(targetUserIds, pageable)
                .map(ReviewResponse::from);
    }

    private Map<Integer, Set<Long>> calculateDegrees(Long userId) {
        return followRepository.findNetworkDegrees(userId, MAX_DEGREE, MAX_NETWORK_SIZE).stream()
                .collect(Collectors.groupingBy(
                        FollowRepository.NetworkDegreeRow::getDegree,
                        Collectors.mapping(FollowRepository.NetworkDegreeRow::getUserId, Collectors.toSet())
                ));
    }
}
