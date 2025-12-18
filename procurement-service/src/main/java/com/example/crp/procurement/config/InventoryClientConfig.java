package com.example.crp.procurement.config;

import com.example.crp.serviceauth.BearerExchangeFilter;
import com.example.crp.serviceauth.ClientCredentialsTokenManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class InventoryClientConfig {

    @Value("${inventory.base-url:http://inventory-service:8082}")
    private String inventoryBaseUrl;

    @Value("${OIDC_ISSUER:http://keycloak:8080/realms/crp}")
    private String issuer;

    @Value("${S2S_CLIENT_ID:procurement-s2s}")
    private String clientId;

    @Value("${S2S_CLIENT_SECRET:procurement-s2s-secret}")
    private String clientSecret;

    @Bean
    public WebClient inventoryS2SClient(WebClient.Builder builder, ReactiveStringRedisTemplate redis) {
        ClientCredentialsTokenManager tm = new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret, redis, "s2s:cc");
        return builder.clone().baseUrl(inventoryBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }
}
