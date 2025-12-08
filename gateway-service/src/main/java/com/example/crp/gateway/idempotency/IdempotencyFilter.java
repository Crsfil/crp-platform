package com.example.crp.gateway.idempotency;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Simple API-level idempotency filter.
 * <p>
 * For mutating requests (POST/PUT/PATCH) that contain an Idempotency-Key header,
 * the filter stores the key in Redis with a limited TTL. If the same key is
 * seen again for the same HTTP method and path within the TTL window, the
 * request is short-circuited and a "duplicate" response is returned without
 * hitting downstream services.
 */
@Component
public class IdempotencyFilter implements GlobalFilter, Ordered {
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_HEADER_LEGACY = "X-Idempotency-Key";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Duration ttl;

    public IdempotencyFilter(ReactiveStringRedisTemplate redisTemplate,
                             @Value("${gateway.idempotency.ttl:PT24H}") String ttl) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.parse(ttl);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpMethod method = exchange.getRequest().getMethod();
        if (method == null || !isMutating(method)) {
            return chain.filter(exchange);
        }

        String key = resolveKey(exchange);
        if (key == null || key.isBlank()) {
            return chain.filter(exchange);
        }

        String redisKey = buildRedisKey(exchange, key);
        return redisTemplate
                .opsForValue()
                .setIfAbsent(redisKey, "1", ttl)
                .flatMap(isNew -> {
                    if (Boolean.FALSE.equals(isNew)) {
                        return duplicateResponse(exchange);
                    }
                    return chain.filter(exchange);
                });
    }

    private static boolean isMutating(HttpMethod method) {
        return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
    }

    private String resolveKey(ServerWebExchange exchange) {
        var headers = exchange.getRequest().getHeaders();
        String key = headers.getFirst(IDEMPOTENCY_HEADER);
        if (key == null || key.isBlank()) {
            key = headers.getFirst(IDEMPOTENCY_HEADER_LEGACY);
        }
        return key;
    }

    private String buildRedisKey(ServerWebExchange exchange, String key) {
        String method = exchange.getRequest().getMethodValue();
        String path = exchange.getRequest().getPath().value();
        return "idem:" + method + ":" + path + ":" + key;
    }

    private Mono<Void> duplicateResponse(ServerWebExchange exchange) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.CONFLICT);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":\"duplicate\",\"reason\":\"idempotency_key_reused\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run after auth filters, but before routing.
        return -50;
    }
}

