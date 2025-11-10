package com.example.crp.reports.config;

import com.example.crp.serviceauth.BearerExchangeFilter;
import com.example.crp.serviceauth.ClientCredentialsTokenManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class S2SClientsConfig {

    @Value("${inventory.base-url:http://inventory-service:8082}")
    private String inventoryBaseUrl;

    @Value("${procurement.base-url:http://procurement-service:8083}")
    private String procurementBaseUrl;

    @Value("${OIDC_ISSUER:http://keycloak:8080/realms/crp}")
    private String issuer;

    @Value("${S2S_CLIENT_ID:reports-service}")
    private String clientId;

    @Value("${S2S_CLIENT_SECRET:reports-secret}")
    private String clientSecret;

    @Bean
    public WebClient inventoryClient(WebClient.Builder builder) {
        ClientCredentialsTokenManager tm = new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret);
        return builder.clone().baseUrl(inventoryBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }

    @Bean
    public WebClient procurementClient(WebClient.Builder builder) {
        ClientCredentialsTokenManager tm = new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret);
        return builder.clone().baseUrl(procurementBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }
}

