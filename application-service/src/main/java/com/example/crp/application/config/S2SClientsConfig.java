package com.example.crp.application.config;

import com.example.crp.serviceauth.BearerExchangeFilter;
import com.example.crp.serviceauth.ClientCredentialsTokenManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class S2SClientsConfig {

    @Value("${kyc.base-url:http://kyc-service:8086}")
    private String kycBaseUrl;
    @Value("${underwriting.base-url:http://underwriting-service:8087}")
    private String uwBaseUrl;
    @Value("${pricing.base-url:http://product-pricing-service:8088}")
    private String pricingBaseUrl;

    @Value("${OIDC_ISSUER:http://keycloak:8080/realms/crp}")
    private String issuer;

    @Value("${S2S_CLIENT_ID:application-service-caller}")
    private String clientId;

    @Value("${S2S_CLIENT_SECRET:application-secret}")
    private String clientSecret;

    private ClientCredentialsTokenManager tokenManager(WebClient.Builder builder) {
        return new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret);
    }

    @Bean
    public WebClient kycClient(WebClient.Builder builder) {
        var tm = tokenManager(builder);
        return builder.clone().baseUrl(kycBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }

    @Bean
    public WebClient underwritingClient(WebClient.Builder builder) {
        var tm = tokenManager(builder);
        return builder.clone().baseUrl(uwBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }

    @Bean
    public WebClient pricingClient(WebClient.Builder builder) {
        var tm = tokenManager(builder);
        return builder.clone().baseUrl(pricingBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }
}

