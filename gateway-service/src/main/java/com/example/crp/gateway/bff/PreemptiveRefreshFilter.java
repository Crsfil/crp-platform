package com.example.crp.gateway.bff;

import com.example.crp.gateway.refresh.SingleFlightRefreshManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Component
public class PreemptiveRefreshFilter implements GlobalFilter, Ordered {
    private final BffProperties props;
    private final SingleFlightRefreshManager singleFlight;
    private final BffTokenService tokenService;
    private final MeterRegistry meterRegistry;

    public PreemptiveRefreshFilter(BffProperties props, SingleFlightRefreshManager singleFlight, BffTokenService tokenService, MeterRegistry meterRegistry) {
        this.props = props; this.singleFlight = singleFlight; this.tokenService = tokenService; this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/bff/") || path.startsWith("/oidc/") || path.startsWith("/auth/") || path.startsWith("/swagger") || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }
        String token = auth.substring(7);
        if (!isExpiring(token, 30)) {
            return chain.filter(exchange);
        }
        var rtCookie = exchange.getRequest().getCookies().getFirst(props.getCookie().getName());
        if (rtCookie == null) {
            return chain.filter(exchange);
        }
        String refresh = rtCookie.getValue();
        String sessionId = sha256(refresh);
        meterRegistry.counter("gateway.refresh.preemptive", Tags.of("event","triggered")).increment();
        return singleFlight.refresh(sessionId, () -> tokenService.refresh(refresh))
                .flatMap(tp -> {
                    meterRegistry.counter("gateway.refresh.preemptive", Tags.of("event","success")).increment();
                    // set new cookie
                    ResponseCookie cookie = ResponseCookie.from(props.getCookie().getName(), tp.refreshToken())
                            .httpOnly(true)
                            .secure(props.getCookie().isSecure())
                            .sameSite(props.getCookie().getSameSite())
                            .path("/")
                            .maxAge(Duration.ofHours(1))
                            .build();
                    exchange.getResponse().addCookie(cookie);
                    // mutate request with new AT
                    var mutated = exchange.mutate()
                            .request(r -> r.headers(h -> h.setBearerAuth(tp.accessToken())))
                            .build();
                    return chain.filter(mutated);
                })
                .onErrorResume(e -> {
                    meterRegistry.counter("gateway.refresh.preemptive", Tags.of("event","error")).increment();
                    return chain.filter(exchange);
                });
    }

    @Override public int getOrder() { return -50; }

    private static boolean isExpiring(String jwt, int leewaySeconds) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return false;
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            int idx = payloadJson.indexOf("\"exp\"");
            if (idx < 0) return false;
            // very small JSON parse: find first number after "exp":
            int colon = payloadJson.indexOf(':', idx);
            if (colon < 0) return false;
            int end = colon + 1;
            while (end < payloadJson.length() && Character.isWhitespace(payloadJson.charAt(end))) end++;
            int start = end;
            while (end < payloadJson.length() && Character.isDigit(payloadJson.charAt(end))) end++;
            long exp = Long.parseLong(payloadJson.substring(start, end));
            long now = Instant.now().getEpochSecond();
            return exp <= now + leewaySeconds;
        } catch (Exception e) {
            return false;
        }
    }

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
