package com.example.crp.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisTokenStore {
    private final StringRedisTemplate redis;
    public RedisTokenStore(StringRedisTemplate redis) { this.redis = redis; }

    public void storeRefresh(String tokenId, long ttlSeconds) {
        redis.opsForValue().set(refreshKey(tokenId), "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isRefreshActive(String tokenId) {
        String v = redis.opsForValue().get(refreshKey(tokenId));
        return v != null;
    }

    public void revokeRefresh(String tokenId) {
        redis.delete(refreshKey(tokenId));
    }

    private String refreshKey(String tokenId) { return "refresh:" + tokenId; }
}

