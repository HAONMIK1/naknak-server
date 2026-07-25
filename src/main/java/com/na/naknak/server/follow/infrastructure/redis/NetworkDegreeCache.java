package com.na.naknak.server.follow.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 유저별 촌수(1~3) 계산 결과를 캐싱한다. 팔로우 그래프를 매 요청마다 다시 훑는 비용을 없애기 위함
 * (100만 유저 규모 가정 - docs/feed.md 참고).
 *
 * 정합성은 TTL(10분) + 팔로우/언팔로우 시 본인 캐시 즉시 무효화로 맞춘다. "나를 거쳐서 촌수가
 * 바뀐 제3자"의 캐시까지 실시간으로 무효화하지는 않는다 — 최대 TTL만큼의 지연을 감수한다.
 */
@Repository
@RequiredArgsConstructor
public class NetworkDegreeCache {

    private static final String KEY_PREFIX = "NW:";
    private static final long TTL_SECONDS = 600;
    private static final int MAX_DEGREE = 3;

    private final RedissonClient redissonClient;

    public Optional<Map<Integer, Set<Long>>> find(Long userId) {
        if (!redissonClient.getBucket(sentinelKey(userId)).isExists()) {
            return Optional.empty();
        }

        Map<Integer, Set<Long>> degreeGroups = new HashMap<>();
        for (int degree = 1; degree <= MAX_DEGREE; degree++) {
            RSet<Long> set = redissonClient.getSet(degreeKey(userId, degree));
            degreeGroups.put(degree, new HashSet<>(set.readAll()));
        }
        return Optional.of(degreeGroups);
    }

    public void save(Long userId, Map<Integer, Set<Long>> degreeGroups) {
        for (int degree = 1; degree <= MAX_DEGREE; degree++) {
            RSet<Long> set = redissonClient.getSet(degreeKey(userId, degree));
            set.delete();
            Set<Long> members = degreeGroups.getOrDefault(degree, Set.of());
            if (!members.isEmpty()) {
                set.addAll(members);
            }
            set.expire(Duration.ofSeconds(TTL_SECONDS));
        }
        redissonClient.<String>getBucket(sentinelKey(userId))
                .set("1", TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void invalidate(Long userId) {
        redissonClient.getBucket(sentinelKey(userId)).delete();
        for (int degree = 1; degree <= MAX_DEGREE; degree++) {
            redissonClient.getSet(degreeKey(userId, degree)).delete();
        }
    }

    private String sentinelKey(Long userId) {
        return KEY_PREFIX + userId + ":computed";
    }

    private String degreeKey(Long userId, int degree) {
        return KEY_PREFIX + userId + ":d" + degree;
    }
}
