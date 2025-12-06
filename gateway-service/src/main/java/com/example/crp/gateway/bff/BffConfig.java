package com.example.crp.gateway.bff;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(BffProperties.class)
public class BffConfig {
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .exchangeStrategies(ExchangeStrategies.builder().codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024)).build())
                .build();
    }

    @Bean
    public Cache<String, BffTokenService.CachedAccess> accessTokenCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .build();
    }
}
