package com.example.crp.inventory.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    void cacheManagerIsCreatedWithLettuceFactory() {
        LettuceConnectionFactory factory = mock(LettuceConnectionFactory.class);

        CacheManager cacheManager = cacheConfig.cacheManager(factory);

        assertThat(cacheManager).isNotNull();
    }

    @Test
    void redisTemplateUsesProvidedConnectionFactory() {
        LettuceConnectionFactory factory = mock(LettuceConnectionFactory.class);

        RedisTemplate<String, String> template = cacheConfig.redisTemplate(factory);

        assertThat(template.getConnectionFactory()).isEqualTo(factory);
    }
}

