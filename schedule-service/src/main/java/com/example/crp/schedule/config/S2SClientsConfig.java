package com.example.crp.schedule.config;

import com.example.crp.serviceauth.BearerExchangeFilter;
import com.example.crp.serviceauth.ClientCredentialsTokenManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class S2SClientsConfig {
    @Value("${pricing.base-url:http://product-pricing-service:8088}")
    private String pricingBaseUrl;

    @Value("${OIDC_ISSUER:http://keycloak:8080/realms/crp}")
    private String issuer;

    @Value("${S2S_CLIENT_ID:schedule-service-caller}")
    private String clientId;

    @Value("${S2S_CLIENT_SECRET:schedule-secret}")
    private String clientSecret;

    @Bean
    public WebClient pricingClient(WebClient.Builder builder) {
        ClientCredentialsTokenManager tm = new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret);
        return builder.clone().baseUrl(pricingBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }
}

