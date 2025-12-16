package com.example.crp.application.config;

import com.example.crp.serviceauth.BearerExchangeFilter;
import com.example.crp.serviceauth.ClientCredentialsTokenManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class S2SClientsConfig {

    @Value("${kyc.base-url:http://kyc-service:8086}")
    private String kycBaseUrl;
    @Value("${underwriting.base-url:http://underwriting-service:8087}")
    private String uwBaseUrl;
    @Value("${pricing.base-url:http://product-pricing-service:8088}")
    private String pricingBaseUrl;
    @Value("${bpm.base-url:http://bpm-service:8095}")
    private String bpmBaseUrl;

    @Value("${OIDC_ISSUER:http://keycloak:8080/realms/crp}")
    private String issuer;

    @Value("${S2S_CLIENT_ID:application-service-caller}")
    private String clientId;

    @Value("${S2S_CLIENT_SECRET:application-secret}")
    private String clientSecret;

    @Bean
    @Primary
    public WebClient.Builder instrumentedBuilder() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(2))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 1_000);
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    private ClientCredentialsTokenManager tokenManager(WebClient.Builder builder) {
        return new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret);
    }

    @Bean
    public WebClient kycClient(WebClient.Builder instrumentedBuilder) {
        var tm = tokenManager(instrumentedBuilder);
        return instrumentedBuilder.clone().baseUrl(kycBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }

    @Bean
    public WebClient underwritingClient(WebClient.Builder instrumentedBuilder) {
        var tm = tokenManager(instrumentedBuilder);
        return instrumentedBuilder.clone().baseUrl(uwBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }

    @Bean
    public WebClient pricingClient(WebClient.Builder instrumentedBuilder) {
        var tm = tokenManager(instrumentedBuilder);
        return instrumentedBuilder.clone().baseUrl(pricingBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }

    @Bean
    public WebClient bpmClient(WebClient.Builder instrumentedBuilder) {
        var tm = tokenManager(instrumentedBuilder);
        return instrumentedBuilder.clone().baseUrl(bpmBaseUrl).filter(new BearerExchangeFilter(tm)).build();
    }
}
