package com.example.crp.bpm.config;

import com.example.crp.serviceauth.BearerExchangeFilter;
import com.example.crp.serviceauth.ClientCredentialsTokenManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.time.Duration;

@Configuration
public class S2SClientsConfig {
    @Value("${kyc.base-url:http://kyc-service:8086}")
    private String kycBaseUrl;
    @Value("${underwriting.base-url:http://underwriting-service:8087}")
    private String uwBaseUrl;
    @Value("${pricing.base-url:http://product-pricing-service:8088}")
    private String pricingBaseUrl;
    @Value("${application.base-url:http://application-service:8089}")
    private String applicationBaseUrl;

    @Value("${OIDC_ISSUER:http://keycloak:8080/realms/crp}")
    private String issuer;
    @Value("${S2S_CLIENT_ID:bpm-service-caller}")
    private String clientId;
    @Value("${S2S_CLIENT_SECRET:bpm-secret}")
    private String clientSecret;

    private BearerExchangeFilter bearer(WebClient.Builder builder){
        return new BearerExchangeFilter(new ClientCredentialsTokenManager(builder.build(), issuer, clientId, clientSecret));
    }

    @Bean
    @Primary
    public WebClient.Builder instrumentedBuilder() {
        TcpClient tcpClient = TcpClient.create()
                .responseTimeout(Duration.ofSeconds(2))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 1_000);
        HttpClient httpClient = HttpClient.from(tcpClient);
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Bean public WebClient kycClient(WebClient.Builder b){ return b.clone().baseUrl(kycBaseUrl).filter(bearer(b)).build(); }
    @Bean public WebClient underwritingClient(WebClient.Builder b){ return b.clone().baseUrl(uwBaseUrl).filter(bearer(b)).build(); }
    @Bean public WebClient pricingClient(WebClient.Builder b){ return b.clone().baseUrl(pricingBaseUrl).filter(bearer(b)).build(); }
    @Bean public WebClient applicationClient(WebClient.Builder b){ return b.clone().baseUrl(applicationBaseUrl).filter(bearer(b)).build(); }
}
