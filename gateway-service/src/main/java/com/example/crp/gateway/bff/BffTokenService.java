package com.example.crp.gateway.bff;

import com.example.crp.gateway.refresh.SingleFlightRefreshManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

@Service
public class BffTokenService {
    private static final String REDIS_KEY_FMT = "bff:at:%s";

    private final WebClient webClient;
    private final BffProperties props;
    private final Cache<String, CachedAccess> l1Cache;
    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    public BffTokenService(WebClient oidcWebClient,
                           BffProperties props,
                           Cache<String, CachedAccess> l1Cache,
                           ReactiveStringRedisTemplate redis) {
        this.webClient = oidcWebClient;
        this.props = props;
        this.l1Cache = l1Cache;
        this.redis = redis;
    }

    public Mono<SingleFlightRefreshManager.TokenPair> refresh(String refreshToken){
        Objects.requireNonNull(refreshToken, "refreshToken");
        String sessionId = sha256(refreshToken);
        long skewSeconds = props.getProactiveRefreshSkewSeconds();
        Instant now = Instant.now();

        // L1: in-memory (Caffeine)
        CachedAccess cached = l1Cache.getIfPresent(sessionId);
        if (cached != null && cached.exp.isAfter(now.plusSeconds(skewSeconds))) {
            return Mono.just(new SingleFlightRefreshManager.TokenPair(cached.accessToken, refreshToken));
        }

        // L2: Redis
        return redis.opsForValue().get(redisKey(sessionId))
                .flatMap(json -> fromJson(json, refreshToken, skewSeconds))
                .switchIfEmpty(doRefresh(refreshToken, sessionId, skewSeconds))
                .onErrorResume(e -> doRefresh(refreshToken, sessionId, skewSeconds));
    }

    private Mono<SingleFlightRefreshManager.TokenPair> doRefresh(String refreshToken, String sessionId, long skewSeconds) {
        String tokenEndpoint = props.getIssuer().endsWith("/")
                ? props.getIssuer() + "protocol/openid-connect/token"
                : props.getIssuer() + "/protocol/openid-connect/token";
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", Objects.requireNonNull(props.getClientId()));
        form.add("refresh_token", refreshToken);

        return webClient.post()
                .uri(tokenEndpoint)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(TokenEndpointResponse.class)
                .flatMap(tr -> {
                    String newRt = tr.refreshToken != null ? tr.refreshToken : refreshToken;
                    String accessToken = tr.accessToken;
                    long expiresIn = tr.expiresIn > 0 ? tr.expiresIn : 300;
                    Instant exp = Instant.now().plusSeconds(expiresIn);

                    String newSessionId = sha256(newRt);
                    CachedAccess ca = new CachedAccess(accessToken, exp);
                    l1Cache.put(newSessionId, ca);
                    Duration ttl = Duration.between(Instant.now(), exp.minusSeconds(skewSeconds <= 0 ? 0 : skewSeconds));
                    if (!ttl.isNegative() && !ttl.isZero()) {
                        redis.opsForValue().set(redisKey(newSessionId), toJson(ca), ttl).subscribe();
                    }
                    if (!newSessionId.equals(sessionId)) {
                        l1Cache.invalidate(sessionId);
                        redis.delete(redisKey(sessionId)).subscribe();
                    }
                    return Mono.just(new SingleFlightRefreshManager.TokenPair(accessToken, newRt));
                });
    }

    private Mono<SingleFlightRefreshManager.TokenPair> fromJson(String json, String refreshToken, long skewSeconds) {
        try {
            CachedAccess ca = mapper.readValue(json, CachedAccess.class);
            if (ca.exp.isAfter(Instant.now().plusSeconds(skewSeconds))) {
                l1Cache.put(sha256(refreshToken), ca);
                return Mono.just(new SingleFlightRefreshManager.TokenPair(ca.accessToken, refreshToken));
            }
            return Mono.empty();
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    private String toJson(CachedAccess ca) {
        try { return mapper.writeValueAsString(ca); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String redisKey(String sessionId) { return String.format(REDIS_KEY_FMT, sessionId); }

    static class TokenEndpointResponse {
        @JsonProperty("access_token") String accessToken;
        @JsonProperty("refresh_token") String refreshToken;
        @JsonProperty("expires_in") int expiresIn;
        @JsonProperty("token_type") String tokenType;
        @JsonProperty("scope") String scope;
    }

    static class CachedAccess {
        public String accessToken;
        public Instant exp;

        public CachedAccess() {}
        CachedAccess(String accessToken, Instant exp) { this.accessToken = accessToken; this.exp = exp; }
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            return s;
        }
    }
}

