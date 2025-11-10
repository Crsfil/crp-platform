package com.example.crp.gateway.bff;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.example.crp.gateway.bff.AuthController.COOKIE_REFRESH;

@Component
public class BffAuthFilter implements GlobalFilter, Ordered {
    private final TokenService tokenService;
    private final BffProperties props;

    public BffAuthFilter(TokenService tokenService, BffProperties props) { this.tokenService = tokenService; this.props = props; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/auth/") || path.startsWith("/oidc/") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            return chain.filter(exchange);
        }
        String refresh = exchange.getRequest().getCookies().getFirst(COOKIE_REFRESH) != null ? exchange.getRequest().getCookies().getFirst(COOKIE_REFRESH).getValue() : null;
        if (refresh == null || refresh.isBlank()) {
            return chain.filter(exchange);
        }
        TokenService.MonoAccess acc;
        try {
            acc = tokenService.ensureAccess(refresh, props.getProactiveRefreshSkewSeconds());
        } catch (Exception ex) {
            return chain.filter(exchange);
        }
        var mutated = exchange.mutate().request(r -> r.headers(h -> h.set(HttpHeaders.AUTHORIZATION, "Bearer "+acc.accessToken()))).build();
        if (acc.rotatedRefresh()) {
            ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(COOKIE_REFRESH, acc.refreshToken()).httpOnly(true).path("/");
            if (props.getCookie().getDomain() != null) b.domain(props.getCookie().getDomain());
            if (props.getCookie().isSecure()) b.secure(true);
            String ss = props.getCookie().getSameSite(); if (ss != null) b.sameSite(ss);
            b.maxAge(Duration.ofDays(30));
            mutated.getResponse().addCookie(b.build());
        }
        return chain.filter(mutated).then(Mono.defer(() -> {
            if (mutated.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // best-effort: clear access cache; next request will refresh
                ResponseCookie clear = ResponseCookie.from(COOKIE_REFRESH, "").httpOnly(true).path("/").maxAge(Duration.ZERO).build();
                mutated.getResponse().addCookie(clear);
            }
            return Mono.empty();
        }));
    }

    @Override public int getOrder() { return -100; }
}

