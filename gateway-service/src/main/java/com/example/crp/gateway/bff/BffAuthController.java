package com.example.crp.gateway.bff;

import com.example.crp.gateway.refresh.SingleFlightRefreshManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

@RestController
@RequestMapping("/bff")
public class BffAuthController {

    private final BffProperties props;
    private final SingleFlightRefreshManager singleFlight;
    private final BffTokenService tokenService;
    private final MeterRegistry meterRegistry;

    public BffAuthController(BffProperties props, SingleFlightRefreshManager singleFlight, BffTokenService tokenService, MeterRegistry meterRegistry) {
        this.props = props;
        this.singleFlight = singleFlight;
        this.tokenService = tokenService;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<java.util.Map<String,Object>>> refresh(ServerWebExchange exchange) {
        String cookieName = props.getCookie().getName();
        var tokenCookie = exchange.getRequest().getCookies().getFirst(cookieName);
        if (tokenCookie == null || tokenCookie.getValue() == null || tokenCookie.getValue().isBlank()) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error","missing refresh cookie")));
        }
        String refreshToken = tokenCookie.getValue();
        String sessionId = sha256(refreshToken);

        meterRegistry.counter("bff.refresh.endpoint", Tags.of("event","attempt")).increment();
        return singleFlight.refresh(sessionId, () -> tokenService.refresh(refreshToken))
                .flatMap(tp -> setRefreshCookie(exchange, tp.refreshToken())
                        .then(Mono.fromCallable(() -> {
                            meterRegistry.counter("bff.refresh.endpoint", Tags.of("event","success")).increment();
                            java.util.Map<String,Object> resp = new java.util.LinkedHashMap<>();
                            resp.put("access_token", tp.accessToken());
                            resp.put("expires_in", 900);
                            return ResponseEntity.ok(resp);
                        })))
                .onErrorResume(e -> {
                    meterRegistry.counter("bff.refresh.endpoint", Tags.of("event","error")).increment();
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error","refresh failed")));
                });
    }

    private Mono<Void> setRefreshCookie(ServerWebExchange exchange, String refreshToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(props.getCookie().getName(), refreshToken)
                .httpOnly(true)
                .secure(props.getCookie().isSecure())
                .path("/")
                .maxAge(Duration.ofHours(1));
        String sameSite = props.getCookie().getSameSite();
        if (sameSite != null && !sameSite.isBlank()) {
            builder = builder.sameSite(sameSite);
        }
        exchange.getResponse().addCookie(builder.build());
        return Mono.empty();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // unified map response is used instead of a DTO for simplicity
}
