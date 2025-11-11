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
        if (path.startsWith("/auth/") || path.startsWith("/oidc/") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.equals("/actuator/health")) {
            return chain.filter(exchange);
        }
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String refresh = exchange.getRequest().getCookies().getFirst(COOKIE_REFRESH) != null ? exchange.getRequest().getCookies().getFirst(COOKIE_REFRESH).getValue() : null;
        if ((refresh == null || refresh.isBlank()) && (authHeader == null || authHeader.isBlank())) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        TokenService.MonoAccess acc;
        ServerWebExchange mutated = exchange;
        if (refresh != null && !refresh.isBlank()) {
            try {
                acc = tokenService.ensureAccess(refresh, props.getProactiveRefreshSkewSeconds());
            } catch (Exception ex) {
                // if refresh flow failed but Authorization header was present, proceed; otherwise 401
                if (authHeader == null || authHeader.isBlank()) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }
                return chain.filter(exchange);
            }
            mutated = exchange.mutate().request(r -> r.headers(h -> h.set(HttpHeaders.AUTHORIZATION, "Bearer "+acc.accessToken()))).build();
            if (acc.rotatedRefresh()) {
                ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(COOKIE_REFRESH, acc.refreshToken()).httpOnly(true).path("/");
                if (props.getCookie().getDomain() != null) b.domain(props.getCookie().getDomain());
                if (props.getCookie().isSecure()) b.secure(true);
                String ss = props.getCookie().getSameSite(); if (ss != null) b.sameSite(ss);
                b.maxAge(Duration.ofDays(30));
                mutated.getResponse().addCookie(b.build());
            }
        }
        ServerWebExchange exToUse = mutated;
        return chain.filter(exToUse).then(Mono.defer(() -> {
            if (exToUse.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // best-effort: clear access cache; next request will refresh
                ResponseCookie clear = ResponseCookie.from(COOKIE_REFRESH, "").httpOnly(true).path("/").maxAge(Duration.ZERO).build();
                exToUse.getResponse().addCookie(clear);
            }
            return Mono.empty();
        }));
    }

    @Override public int getOrder() { return -100; }
}
