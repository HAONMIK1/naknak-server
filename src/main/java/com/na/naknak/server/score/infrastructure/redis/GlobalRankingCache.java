package com.na.naknak.server.score.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 전체(GLOBAL) 랭킹을 Redis ZSET(RScoredSortedSet)으로 상시 유지한다. 모든 유저가 같은 결과를
 * 보는 조회라 캐시 효율이 가장 높아 GLOBAL에만 적용한다(docs/score.md 참고).
 *
 * 매 조회마다 재계산하지 않고, 점수가 바뀌는 시점(ScoreService.earn)에 증분(addScore)만
 * 반영한다. 콜드스타트(Redis flush 등)를 대비해 sentinel 키로 최초 1회 DB backfill을 보장한다.
 */
@Repository
@RequiredArgsConstructor
public class GlobalRankingCache {

    private static final String ZSET_KEY = "RANK:GLOBAL";
    private static final String SEEDED_KEY = "RANK:GLOBAL:seeded";

    private final RedissonClient redissonClient;

    public boolean isSeeded() {
        return redissonClient.getBucket(SEEDED_KEY).isExists();
    }

    public void backfill(Map<Long, Integer> scoresByUserId) {
        RScoredSortedSet<Long> zset = zset();
        zset.delete();
        scoresByUserId.forEach((userId, score) -> zset.add(score, userId));
        redissonClient.<String>getBucket(SEEDED_KEY).set("1");
    }

    public void addScore(Long userId, int delta) {
        zset().addScore(userId, delta);
    }

    public Collection<ScoredEntry<Long>> getTop(int n) {
        return zset().entryRangeReversed(0, n - 1);
    }

    public Optional<Integer> getRank(Long userId) {
        Integer rank = zset().revRank(userId);
        return Optional.ofNullable(rank);
    }

    private RScoredSortedSet<Long> zset() {
        return redissonClient.getScoredSortedSet(ZSET_KEY);
    }
}
