package com.example.crp.serviceauth;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal reactive client-credentials token manager with in-memory caching.
 */
public class ClientCredentialsTokenManager {
    private final WebClient webClient;
    private final String tokenEndpoint;
    private final String clientId;
    private final String clientSecret;

    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public ClientCredentialsTokenManager(WebClient webClient, String issuerBaseUrl, String clientId, String clientSecret) {
        this.webClient = webClient;
        this.tokenEndpoint = issuerBaseUrl.endsWith("/") ? issuerBaseUrl + "protocol/openid-connect/token" : issuerBaseUrl + "/protocol/openid-connect/token";
        this.clientId = Objects.requireNonNull(clientId);
        this.clientSecret = Objects.requireNonNull(clientSecret);
    }

    public Mono<String> getAccessToken() {
        CachedToken current = cache.get();
        Instant now = Instant.now();
        if (current != null && current.expiresAt.isAfter(now.plusSeconds(10))) {
            return Mono.just(current.accessToken);
        }
        return requestNewToken().doOnNext(cache::set).map(ct -> ct.accessToken);
    }

    private Mono<CachedToken> requestNewToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        return webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(TokenResponse.class)
                .map(tr -> new CachedToken(tr.access_token, Instant.now().plusSeconds(tr.expires_in)));
    }

    private record CachedToken(String accessToken, Instant expiresAt) {}

    private static class TokenResponse {
        public String access_token;
        public long expires_in;
        public String token_type;
        public String scope;
    }
}

