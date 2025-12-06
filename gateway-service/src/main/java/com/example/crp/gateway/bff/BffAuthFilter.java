package com.example.crp.gateway.bff;

import com.example.crp.gateway.refresh.SingleFlightRefreshManager;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;

@Component
public class BffAuthFilter implements GlobalFilter, Ordered {
    private final BffTokenService tokenService;
    private final SingleFlightRefreshManager singleFlight;
    private final BffProperties props;

    public BffAuthFilter(BffTokenService tokenService, SingleFlightRefreshManager singleFlight, BffProperties props) {
        this.tokenService = tokenService;
        this.singleFlight = singleFlight;
        this.props = props;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/auth/") || path.startsWith("/oidc/") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.equals("/actuator/health")) {
            return chain.filter(exchange);
        }
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        var refreshCookie = exchange.getRequest().getCookies().getFirst(props.getCookie().getName());
        String refresh = refreshCookie != null ? refreshCookie.getValue() : null;
        if ((refresh == null || refresh.isBlank()) && (authHeader == null || authHeader.isBlank())) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        if (refresh == null || refresh.isBlank()) {
            return chain.filter(exchange).then(clearOn401(exchange));
        }
        String sessionId = sha256(refresh);
        return singleFlight.refresh(sessionId, () -> tokenService.refresh(refresh))
                .flatMap(tp -> {
                    ServerWebExchange mutated = exchange.mutate()
                            .request(r -> r.headers(h -> h.set(HttpHeaders.AUTHORIZATION, "Bearer " + tp.accessToken())))
                            .build();
                    if (!tp.refreshToken().equals(refresh)) {
                        mutated.getResponse().addCookie(buildRefreshCookie(tp.refreshToken()));
                    }
                    return chain.filter(mutated).then(clearOn401(mutated));
                })
                .onErrorResume(ex -> {
                    if (authHeader == null || authHeader.isBlank()) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange).then(clearOn401(exchange));
                });
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(props.getCookie().getName(), refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(30));
        if (props.getCookie().getDomain() != null) {
            b = b.domain(props.getCookie().getDomain());
        }
        if (props.getCookie().isSecure()) {
            b = b.secure(true);
        }
        String ss = props.getCookie().getSameSite();
        if (ss != null) {
            b = b.sameSite(ss);
        }
        return b.build();
    }

    private Mono<Void> clearOn401(ServerWebExchange exchange) {
        return Mono.defer(() -> {
            if (exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED) {
                ResponseCookie clear = ResponseCookie.from(props.getCookie().getName(), "")
                        .httpOnly(true)
                        .path("/")
                        .maxAge(Duration.ZERO)
                        .build();
                exchange.getResponse().addCookie(clear);
            }
            return Mono.empty();
        });
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

    @Override public int getOrder() { return -100; }
}
