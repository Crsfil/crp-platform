package com.example.crp.serviceauth;

import org.springframework.http.MediaType;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
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

    private final ReactiveStringRedisTemplate redis;
    private final String redisKeyPrefix;
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicReference<CachedToken> cache = new AtomicReference<>();

    public ClientCredentialsTokenManager(WebClient webClient, String issuerBaseUrl, String clientId, String clientSecret) {
        this(webClient, issuerBaseUrl, clientId, clientSecret, null, "s2s:cc");
    }

    /**
     * Optional Redis-backed L2 cache + Redis singleflight lock to avoid token storms in multi-instance setups.
     */
    public ClientCredentialsTokenManager(WebClient webClient,
                                         String issuerBaseUrl,
                                         String clientId,
                                         String clientSecret,
                                         ReactiveStringRedisTemplate redis,
                                         String redisKeyPrefix) {
        this.webClient = webClient;
        this.tokenEndpoint = issuerBaseUrl.endsWith("/") ? issuerBaseUrl + "protocol/openid-connect/token" : issuerBaseUrl + "/protocol/openid-connect/token";
        this.clientId = Objects.requireNonNull(clientId);
        this.clientSecret = Objects.requireNonNull(clientSecret);
        this.redis = redis;
        this.redisKeyPrefix = (redisKeyPrefix == null || redisKeyPrefix.isBlank()) ? "s2s:cc" : redisKeyPrefix.trim();
    }

    public Mono<String> getAccessToken() {
        CachedToken current = cache.get();
        Instant now = Instant.now();
        if (current != null && current.expiresAt.isAfter(now.plusSeconds(10))) {
            return Mono.just(current.accessToken);
        }
        if (redis == null) {
            return requestNewToken().doOnNext(cache::set).map(ct -> ct.accessToken);
        }
        String keyId = keyId(tokenEndpoint, clientId);
        String tokenKey = redisKeyPrefix + ":token:" + keyId;
        String lockKey = redisKeyPrefix + ":lock:" + keyId;
        return redis.opsForValue().get(tokenKey)
                .flatMap(this::fromJson)
                .switchIfEmpty(acquireAndRequest(tokenKey, lockKey))
                .doOnNext(cache::set)
                .map(ct -> ct.accessToken);
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

    private Mono<CachedToken> acquireAndRequest(String tokenKey, String lockKey) {
        Duration lockTtl = Duration.ofSeconds(30);
        return redis.opsForValue()
                .setIfAbsent(lockKey, "1", lockTtl)
                .flatMap(acquired -> {
                    if (Boolean.TRUE.equals(acquired)) {
                        return requestNewToken()
                                .flatMap(ct -> storeToRedis(tokenKey, ct).thenReturn(ct))
                                .doFinally(sig -> redis.delete(lockKey).subscribe());
                    }
                    return waitForToken(tokenKey).switchIfEmpty(requestNewToken()
                            .flatMap(ct -> storeToRedis(tokenKey, ct).thenReturn(ct)));
                });
    }

    private Mono<CachedToken> waitForToken(String tokenKey) {
        return Mono.defer(() -> redis.opsForValue().get(tokenKey).flatMap(this::fromJson))
                .repeatWhenEmpty(repeat -> repeat.delayElements(Duration.ofMillis(200)).take(15))
                .timeout(Duration.ofSeconds(5))
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<Void> storeToRedis(String tokenKey, CachedToken ct) {
        String json = toJson(ct);
        Instant now = Instant.now();
        Duration ttl = Duration.between(now, ct.expiresAt.minusSeconds(10));
        if (ttl.isNegative() || ttl.isZero()) {
            return redis.opsForValue().set(tokenKey, json).then();
        }
        return redis.opsForValue().set(tokenKey, json, ttl).then();
    }

    private Mono<CachedToken> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Mono.empty();
        }
        try {
            CachedToken ct = mapper.readValue(json, CachedToken.class);
            if (ct.expiresAt == null || ct.accessToken == null || ct.accessToken.isBlank()) {
                return Mono.empty();
            }
            if (ct.expiresAt.isAfter(Instant.now().plusSeconds(10))) {
                return Mono.just(ct);
            }
            return Mono.empty();
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    private String toJson(CachedToken ct) {
        try {
            return mapper.writeValueAsString(ct);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private record CachedToken(String accessToken, Instant expiresAt) {}

    private static class TokenResponse {
        public String access_token;
        public long expires_in;
        public String token_type;
        public String scope;
    }

    private static String keyId(String tokenEndpoint, String clientId) {
        String raw = tokenEndpoint + "|" + clientId;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }
}

