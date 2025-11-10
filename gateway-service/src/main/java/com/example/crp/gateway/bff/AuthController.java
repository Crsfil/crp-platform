package com.example.crp.gateway.bff;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Controller
public class AuthController {
    static final String COOKIE_REFRESH = "refresh_token";
    static final String COOKIE_STATE = "oauth2_state";
    static final String COOKIE_VERIFIER = "oauth2_verifier";

    private final BffProperties props;
    private final WebClient webClient;

    public AuthController(BffProperties props, WebClient webClient) {
        this.props = props; this.webClient = webClient;
    }

    @GetMapping("/auth/login")
    public ResponseEntity<Void> login(@RequestParam(value = "redirect_uri", required = false) String redirectBack) {
        String issuer = props.getIssuer();
        String authEndpoint = issuer + "/protocol/openid-connect/auth";
        String redirectUri = (redirectBack != null && !redirectBack.isBlank()) ? redirectBack : defaultCallback();
        String state = randomUrlSafe(32);
        String verifier = randomUrlSafe(64);
        String challenge = pkceChallenge(verifier);
        URI location = UriComponentsBuilder.fromUriString(authEndpoint)
                .queryParam("response_type", "code")
                .queryParam("client_id", props.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", props.getScopes())
                .queryParam("state", state)
                .queryParam("code_challenge_method", "S256")
                .queryParam("code_challenge", challenge)
                .build(true).toUri();

        ResponseCookie stateC = baseCookie(COOKIE_STATE, state).maxAge(Duration.ofMinutes(5)).build();
        ResponseCookie verC = baseCookie(COOKIE_VERIFIER, verifier).maxAge(Duration.ofMinutes(5)).build();
        return ResponseEntity.status(302).location(location)
                .header(HttpHeaders.SET_COOKIE, stateC.toString())
                .header(HttpHeaders.SET_COOKIE, verC.toString())
                .build();
    }

    @GetMapping("/auth/callback")
    public Mono<ResponseEntity<Void>> callback(@RequestParam MultiValueMap<String,String> params,
                                               @org.springframework.web.bind.annotation.CookieValue(value = COOKIE_STATE, required = false) String stateCookie,
                                               @org.springframework.web.bind.annotation.CookieValue(value = COOKIE_VERIFIER, required = false) String verifierCookie,
                                               @RequestParam(value="redirect_uri", required=false) String redirectBack) {
        String code = params.getFirst("code");
        String state = params.getFirst("state");
        if (code == null || state == null || stateCookie == null || !state.equals(stateCookie) || verifierCookie == null) {
            return Mono.just(ResponseEntity.status(400).build());
        }
        String tokenEndpoint = props.getIssuer()+"/protocol/openid-connect/token";
        String redirectUri = (redirectBack != null && !redirectBack.isBlank()) ? redirectBack : defaultCallback();
        return webClient.post().uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type","authorization_code")
                        .with("code", code)
                        .with("client_id", props.getClientId())
                        .with("redirect_uri", redirectUri)
                        .with("code_verifier", verifierCookie))
                .retrieve()
                .bodyToMono(Map.class)
                .map(body -> {
                    String refresh = (String) body.get("refresh_token");
                    Long expiresIn = ((Number) body.getOrDefault("refresh_expires_in", 0)).longValue();
                    ResponseCookie clearState = baseCookie(COOKIE_STATE, "").maxAge(Duration.ZERO).build();
                    ResponseCookie clearVerifier = baseCookie(COOKIE_VERIFIER, "").maxAge(Duration.ZERO).build();
                    ResponseCookie rt = baseCookie(COOKIE_REFRESH, refresh)
                            .maxAge(expiresIn != null && expiresIn > 0 ? Duration.ofSeconds(expiresIn) : Duration.ofDays(30))
                            .build();
                    URI redirect = URI.create("/");
                    return ResponseEntity.status(302).location(redirect)
                            .header(HttpHeaders.SET_COOKIE, clearState.toString())
                            .header(HttpHeaders.SET_COOKIE, clearVerifier.toString())
                            .header(HttpHeaders.SET_COOKIE, rt.toString())
                            .build();
                });
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie rt = baseCookie(COOKIE_REFRESH, "").maxAge(Duration.ZERO).build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, rt.toString()).build();
    }

    private String defaultCallback() { return "http://localhost:8080/auth/callback"; }

    private static String randomUrlSafe(int bytes) {
        byte[] b = new byte[bytes]; new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String pkceChallenge(String verifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .path("/");
        if (props.getCookie().getDomain() != null) b.domain(props.getCookie().getDomain());
        if (props.getCookie().isSecure()) b.secure(true);
        String ss = props.getCookie().getSameSite();
        if (ss != null) b.sameSite(ss);
        return b;
    }
}

