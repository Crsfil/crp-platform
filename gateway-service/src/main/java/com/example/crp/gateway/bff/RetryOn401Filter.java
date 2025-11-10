package com.example.crp.gateway.bff;

import com.example.crp.gateway.refresh.SingleFlightRefreshManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
public class RetryOn401Filter implements GlobalFilter, Ordered {
    private static final String RETRIED_ATTR = RetryOn401Filter.class.getName()+".retried";

    private final BffProperties props;
    private final SingleFlightRefreshManager singleFlight;
    private final BffTokenService tokenService;
    private final MeterRegistry meterRegistry;

    public RetryOn401Filter(BffProperties props, SingleFlightRefreshManager singleFlight, BffTokenService tokenService, MeterRegistry meterRegistry) {
        this.props = props; this.singleFlight = singleFlight; this.tokenService = tokenService; this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (Boolean.TRUE.equals(exchange.getAttribute(RETRIED_ATTR))) {
            return chain.filter(exchange);
        }

        ServerHttpResponse originalResponse = exchange.getResponse();
        ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends org.springframework.core.io.buffer.DataBuffer> body) {
                HttpStatusCode status = getStatusCode();
                if (status != null && status.value() == 401) {
                    var rtCookie = exchange.getRequest().getCookies().getFirst(props.getCookie().getName());
                    if (rtCookie == null || rtCookie.getValue() == null || rtCookie.getValue().isBlank()) {
                        return super.writeWith(body);
                    }
                    String refresh = rtCookie.getValue();
                    String sessionId = sha256(refresh);
                    exchange.getAttributes().put(RETRIED_ATTR, true);
                    meterRegistry.counter("gateway.refresh.retry401", Tags.of("event","triggered")).increment();
                    return singleFlight.refresh(sessionId, () -> tokenService.refresh(refresh))
                            .flatMap(tp -> {
                                meterRegistry.counter("gateway.refresh.retry401", Tags.of("event","success")).increment();
                                // Set new refresh cookie
                                ResponseCookie cookie = ResponseCookie.from(props.getCookie().getName(), tp.refreshToken())
                                        .httpOnly(true)
                                        .secure(props.getCookie().isSecure())
                                        .sameSite(props.getCookie().getSameSite())
                                        .path("/")
                                        .maxAge(Duration.ofHours(1))
                                        .build();
                                originalResponse.addCookie(cookie);
                                // Retry downstream with new AT
                                var mutated = exchange.mutate()
                                        .request(r -> r.headers(h -> h.set(HttpHeaders.AUTHORIZATION, "Bearer " + tp.accessToken())))
                                        .response(originalResponse)
                                        .build();
                                return chain.filter(mutated);
                            })
                            .onErrorResume(e -> {
                                meterRegistry.counter("gateway.refresh.retry401", Tags.of("event","error")).increment();
                                return super.writeWith(body);
                            });
                }
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decorated).build());
    }

    @Override
    public int getOrder() { return -40; }

    private static String sha256(String s) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (Exception e) {
            return s;
        }
    }
}
