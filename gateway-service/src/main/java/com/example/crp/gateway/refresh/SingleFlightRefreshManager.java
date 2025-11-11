package com.example.crp.gateway.refresh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import org.springframework.data.redis.connection.ReturnType;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Redis-based single-flight coordinator for token refresh across gateway instances.
 * Usage:
 *   singleFlight.refresh(sessionId, () -> callIdpRefresh(...))
 */
@Component
public class SingleFlightRefreshManager {
    private static final String LOCK_KEY_FMT = "bff:refresh:lock:%s";
    private static final String RESULT_KEY_FMT = "bff:refresh:result:%s";

    private final ReactiveStringRedisTemplate redis;
    private final MeterRegistry meterRegistry;
    private final Timer refreshTimer;
    private final ObjectMapper mapper = new ObjectMapper();

    public record TokenPair(String accessToken, String refreshToken) {}

    public SingleFlightRefreshManager(ReactiveStringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redis = redisTemplate;
        this.meterRegistry = meterRegistry;
        this.refreshTimer = Timer.builder("bff.refresh.singleflight.timer")
                .description("Time spent performing token refresh (single initiator)")
                .register(meterRegistry);
    }

    public Mono<TokenPair> refresh(String sessionId, java.util.function.Supplier<Mono<TokenPair>> refresher) {
        Objects.requireNonNull(sessionId, "sessionId");
        String lockKey = lockKey(sessionId);
        String resultKey = resultKey(sessionId);
        String lockVal = UUID.randomUUID().toString();

        return tryLock(lockKey, lockVal, Duration.ofSeconds(7))
                .flatMap(locked -> {
                    if (locked) {
                        meterRegistry.counter("bff.refresh.singleflight", Tags.of("event","lock_acquired")).increment();
                        return doRefreshAndPublish(resultKey, lockKey, lockVal, refresher);
                    } else {
                        meterRegistry.counter("bff.refresh.singleflight", Tags.of("event","lock_contended")).increment();
                        return waitForResult(resultKey);
                    }
                })
                .switchIfEmpty(waitForResult(resultKey));
    }

    private Mono<Boolean> tryLock(String key, String value, Duration ttl) {
        return redis.opsForValue().setIfAbsent(key, value, ttl).defaultIfEmpty(false);
    }

    private Mono<TokenPair> doRefreshAndPublish(String resultKey, String lockKey, String lockVal,
                                                java.util.function.Supplier<Mono<TokenPair>> refresher) {
        return refresher.get()
                .flatMap(tp -> redis.opsForValue()
                        .set(resultKey, toJson(tp), Duration.ofSeconds(60))
                        .thenReturn(tp))
                .doOnSuccess(tp -> meterRegistry.counter("bff.refresh.singleflight", Tags.of("event","refresh_success")).increment())
                .transform(mono -> {
                    Timer.Sample sample = Timer.start();
                    return mono.doFinally(sig -> sample.stop(refreshTimer));
                })
                .onErrorResume(e -> {
                    meterRegistry.counter("bff.refresh.singleflight", Tags.of("event","refresh_error")).increment();
                    return redis.opsForValue().set(resultKey, errorJson(e), Duration.ofSeconds(5)).then(Mono.error(e));
                })
                .doFinally(sig -> safelyUnlock(lockKey, lockVal).subscribe());
    }

    private Mono<TokenPair> waitForResult(String resultKey) {
        return redis.opsForValue().get(resultKey)
                .flatMap(this::fromJson)
                .switchIfEmpty(Mono.error(new IllegalStateException("Refresh in progress, result not available")))
                .retryWhen(Retry.fixedDelay(8, Duration.ofMillis(150)))
                .onErrorResume(e -> Mono.error(new IllegalStateException("Refresh single-flight failed to deliver result", e)));
    }

    private Mono<Boolean> safelyUnlock(String key, String expectedVal) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        java.nio.ByteBuffer scr = java.nio.ByteBuffer.wrap(script.getBytes());
        java.nio.ByteBuffer k = java.nio.ByteBuffer.wrap(key.getBytes());
        java.nio.ByteBuffer v = java.nio.ByteBuffer.wrap(expectedVal.getBytes());
        return redis.execute(connection -> connection.scriptingCommands()
                        .eval(scr, ReturnType.BOOLEAN, 1, k, v))
                .next()
                .map(b -> (Boolean) b)
                .defaultIfEmpty(false);
    }

    private String toJson(TokenPair tp) {
        try { return mapper.writeValueAsString(tp); } catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    private String errorJson(Throwable e) {
        return "{\"error\":\"" + e.getClass().getSimpleName() + "\"}"; // simple marker
    }

    private Mono<TokenPair> fromJson(String json) {
        try { return Mono.just(mapper.readValue(json, TokenPair.class)); }
        catch (Exception e) { return Mono.error(e); }
    }

    private static String lockKey(String sessionId) { return String.format(LOCK_KEY_FMT, sessionId); }
    private static String resultKey(String sessionId) { return String.format(RESULT_KEY_FMT, sessionId); }
}
