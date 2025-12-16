package com.example.crp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.HashSet;
import java.util.Set;

public class TrustedClientAuthorizer {

    private final Set<String> trustedClientIds;

    public TrustedClientAuthorizer(Set<String> trustedClientIds) {
        this.trustedClientIds = trustedClientIds == null ? Set.of() : Set.copyOf(trustedClientIds);
    }

    public boolean isTrusted(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return false;
        }
        Jwt jwt = jwtAuth.getToken();
        String clientId = firstNonBlank(
                jwt.getClaimAsString("azp"),
                jwt.getClaimAsString("client_id"),
                jwt.getClaimAsString("clientId")
        );
        if (clientId == null) {
            String preferred = jwt.getClaimAsString("preferred_username");
            if (preferred != null && preferred.startsWith("service-account-")) {
                clientId = preferred.substring("service-account-".length());
            }
        }
        return clientId != null && trustedClientIds.contains(clientId);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    public static Set<String> normalize(Set<String> in) {
        if (in == null || in.isEmpty()) return Set.of();
        Set<String> out = new HashSet<>();
        for (String s : in) {
            if (s == null || s.isBlank()) continue;
            out.add(s.trim());
        }
        return out;
    }
}

