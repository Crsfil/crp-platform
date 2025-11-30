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

    @Value("${agreement.base-url:http://agreement-service:8090}")
    private String agreementBaseUrl;

    @Value("${billing.base-url:http://billing-service:8091}")
    private String billingBaseUrl;

    @Value("${customer.base-url:http://customer-service:8085}")
    private String customerBaseUrl;

    @Value("${application.base-url:http://application-service:8089}")
    private String applicationBaseUrl;

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

    @Bean
    public WebClient agreementClient(WebClient.Builder builder) {
        ClientCredentialsTokenManager tm = new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret);
        return builder.clone().baseUrl(agreementBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }

    @Bean
    public WebClient billingClient(WebClient.Builder builder) {
        ClientCredentialsTokenManager tm = new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret);
        return builder.clone().baseUrl(billingBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }

    @Bean
    public WebClient customerClient(WebClient.Builder builder) {
        ClientCredentialsTokenManager tm = new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret);
        return builder.clone().baseUrl(customerBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }

    @Bean
    public WebClient applicationClient(WebClient.Builder builder) {
        ClientCredentialsTokenManager tm = new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret);
        return builder.clone().baseUrl(applicationBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }
}

