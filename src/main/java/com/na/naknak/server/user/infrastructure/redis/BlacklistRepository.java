package com.na.naknak.server.user.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class BlacklistRepository {

    private static final String KEY_PREFIX = "BL:";

    private final RedissonClient redissonClient;

    public void save(String accessToken, long ttlSeconds) {
        redissonClient.<String>getBucket(KEY_PREFIX + accessToken)
                .set("logout", ttlSeconds, TimeUnit.SECONDS);
    }

    public boolean exists(String accessToken) {
        return redissonClient.getBucket(KEY_PREFIX + accessToken).isExists();
    }
}