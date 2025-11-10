package com.example.crp.gateway.bff;

import com.example.crp.gateway.refresh.SingleFlightRefreshManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
public class BffTokenService {
    private final WebClient webClient;
    private final BffProperties props;

    public BffTokenService(WebClient oidcWebClient, BffProperties props) {
        this.webClient = oidcWebClient; this.props = props;
    }

    public Mono<SingleFlightRefreshManager.TokenPair> refresh(String refreshToken){
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
                .map(tr -> new SingleFlightRefreshManager.TokenPair(tr.accessToken, tr.refreshToken));
    }

    static class TokenEndpointResponse {
        @JsonProperty("access_token") String accessToken;
        @JsonProperty("refresh_token") String refreshToken;
        @JsonProperty("expires_in") int expiresIn;
        @JsonProperty("token_type") String tokenType;
        @JsonProperty("scope") String scope;
    }
}

