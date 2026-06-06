package com.na.naknak.server.user.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "RT:";
    private static final long TTL_DAYS = 14;

    private final RedissonClient redissonClient;

    public void save(Long userId, String refreshToken) {
        redissonClient.<String>getBucket(KEY_PREFIX + userId)
                .set(refreshToken, TTL_DAYS, TimeUnit.DAYS);
    }

    public String find(Long userId) {
        return redissonClient.<String>getBucket(KEY_PREFIX + userId).get();
    }

    public void delete(Long userId) {
        redissonClient.getBucket(KEY_PREFIX + userId).delete();
    }
}