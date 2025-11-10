package com.example.crp.serviceauth;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class BearerExchangeFilter implements ExchangeFilterFunction {
    private final ClientCredentialsTokenManager tokenManager;

    public BearerExchangeFilter(ClientCredentialsTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return tokenManager.getAccessToken()
                .map(token -> ClientRequest.from(request)
                        .headers(h -> h.setBearerAuth(token))
                        .build())
                .flatMap(next::exchange);
    }
}

