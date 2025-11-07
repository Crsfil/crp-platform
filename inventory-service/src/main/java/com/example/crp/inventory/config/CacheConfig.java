package com.example.crp.inventory.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(LettuceConnectionFactory lcf) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5));
        return RedisCacheManager.builder(lcf).cacheDefaults(config).build();
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(LettuceConnectionFactory lcf) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(lcf);
        return t;
    }
}

