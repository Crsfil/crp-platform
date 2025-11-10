package com.example.crp.gateway.bff;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(BffProperties.class)
public class OidcClientConfig {
    @Bean
    public WebClient oidcWebClient(WebClient.Builder builder) {
        // Increase in-memory buffer a bit for token responses (usually small anyway)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
        return builder.exchangeStrategies(strategies).build();
    }
}

