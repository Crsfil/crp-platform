package com.example.crp.billing.config;

import com.example.crp.serviceauth.BearerExchangeFilter;
import com.example.crp.serviceauth.ClientCredentialsTokenManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class S2SClientsConfig {

    @Value("${schedule.base-url:http://schedule-service:8093}")
    private String scheduleBaseUrl;

    @Value("${OIDC_ISSUER:http://keycloak:8080/realms/crp}")
    private String issuer;

    @Value("${S2S_CLIENT_ID:billing-service-caller}")
    private String clientId;

    @Value("${S2S_CLIENT_SECRET:billing-secret}")
    private String clientSecret;

    @Bean
    public WebClient scheduleClient(WebClient.Builder builder) {
        ClientCredentialsTokenManager tm = new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret);
        return builder.clone().baseUrl(scheduleBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }
}

