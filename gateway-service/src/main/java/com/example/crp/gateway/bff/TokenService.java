package com.example.crp.gateway.bff;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@Service
public class TokenService {
    private final BffProperties props;
    private final WebClient webClient;
    private final TokenCache cache = new TokenCache();

    public TokenService(BffProperties props, WebClient webClient) { this.props = props; this.webClient = webClient; }

    public MonoAccess ensureAccess(String refreshToken, long skewSeconds) {
        String key = TokenCache.keyFromRefresh(refreshToken);
        TokenCache.Entry e = cache.get(key);
        if (e != null && e.exp.isAfter(Instant.now().plusSeconds(skewSeconds))) {
            return new MonoAccess(e.accessToken, false, refreshToken);
        }
        // refresh via token endpoint
        Map<String,Object> body = webClient.post().uri(props.getIssuer()+"/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type","refresh_token")
                        .with("refresh_token", refreshToken)
                        .with("client_id", props.getClientId()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (body == null || body.get("access_token") == null) throw new RuntimeException("refresh failed");
        String at = (String) body.get("access_token");
        Number expiresIn = (Number) body.getOrDefault("expires_in", 300);
        String newRt = (String) body.getOrDefault("refresh_token", refreshToken);
        TokenCache.Entry ne = new TokenCache.Entry(at, Instant.now().plusSeconds(expiresIn.longValue()));
        cache.put(TokenCache.keyFromRefresh(newRt), ne);
        if (!newRt.equals(refreshToken)) cache.remove(key);
        return new MonoAccess(at, !newRt.equals(refreshToken), newRt);
    }

    public record MonoAccess(String accessToken, boolean rotatedRefresh, String refreshToken) {}
}

