package com.example.crp.kyc.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Client for calling external KYC / scoring providers with resilience patterns
 * (circuit breaker, retry, rate limiting).
 */
@Component
public class ExternalKycClient {
    private final WebClient webClient;

    public ExternalKycClient(WebClient.Builder builder,
                             @Value("${kyc.external.base-url:http://external-kyc:8080}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    @CircuitBreaker(name = "externalKyc", fallbackMethod = "fallback")
    @Retry(name = "externalKyc")
    @RateLimiter(name = "externalKyc")
    public Decision check(Long customerId) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/external/kyc/check")
                        .queryParam("customerId", customerId)
                        .build())
                .retrieve()
                .bodyToMono(Decision.class)
                .block();
    }

    @SuppressWarnings("unused")
    private Decision fallback(Long customerId, Throwable t) {
        // Degrade gracefully: mark status as UNKNOWN so that upstream can decide what to do.
        return new Decision(customerId, "UNKNOWN");
    }

    public record Decision(Long customerId, String status) {}
}

