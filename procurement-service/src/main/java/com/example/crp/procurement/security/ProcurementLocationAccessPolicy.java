package com.example.crp.procurement.security;

import com.example.crp.security.TrustedClientAuthorizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ProcurementLocationAccessPolicy {
    private final ProcurementAbacProperties props;
    private final TrustedClientAuthorizer trustedClientAuthorizer;
    private final WebClient inventoryClient;

    public ProcurementLocationAccessPolicy(ProcurementAbacProperties props,
                                          TrustedClientAuthorizer trustedClientAuthorizer,
                                          @Qualifier("inventoryS2SClient") WebClient inventoryClient) {
        this.props = props;
        this.trustedClientAuthorizer = trustedClientAuthorizer;
        this.inventoryClient = inventoryClient;
    }

    public void assertLocationWriteAllowed(Authentication auth, Long locationId) {
        if (!isLocationWriteAllowed(auth, locationId)) {
            throw new org.springframework.security.access.AccessDeniedException("location_access_denied");
        }
    }

    public boolean isLocationWriteAllowed(Authentication auth, Long locationId) {
        if (!props.isEnabled()) {
            return true;
        }
        if (locationId == null) {
            return true;
        }
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        if (props.isAdminBypass() && hasRoleAdmin(auth)) {
            return true;
        }
        if (props.isTrustedClientBypass() && trustedClientAuthorizer != null && trustedClientAuthorizer.isTrusted(auth)) {
            return true;
        }
        Jwt jwt = asJwt(auth);
        if (jwt == null) {
            return false;
        }
        Map<String, Object> loc = fetchLocation(locationId);
        if (loc == null) {
            return false;
        }
        String locRegion = normalize(stringVal(loc.get("region")));
        String locBranch = normalize(stringVal(loc.get("code")));
        if (locRegion == null && locBranch == null) {
            return true;
        }
        Set<String> userRegions = claimValues(jwt, "region");
        Set<String> userBranches = claimValues(jwt, "branch");
        if (locRegion != null && userRegions.contains(locRegion)) {
            return true;
        }
        return locBranch != null && userBranches.contains(locBranch);
    }

    private Map<String, Object> fetchLocation(Long id) {
        try {
            return inventoryClient.get()
                    .uri("/locations/{id}", id)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(2));
        } catch (Exception e) {
            return null;
        }
    }

    private static Jwt asJwt(Authentication auth) {
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return null;
        }
        return jwtAuth.getToken();
    }

    private static boolean hasRoleAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(ga.getAuthority()) || "ADMIN".equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> claimValues(Jwt jwt, String claimName) {
        if (jwt == null) return Set.of();
        Object val = jwt.getClaims().get(claimName);
        if (val == null) return Set.of();
        java.util.Set<String> values = new java.util.HashSet<>();
        if (val instanceof Collection<?> collection) {
            for (Object v : collection) {
                String s = normalize(v == null ? null : v.toString());
                if (s != null) values.add(s);
            }
            return values;
        }
        if (val.getClass().isArray()) {
            Object[] arr = (Object[]) val;
            for (Object v : arr) {
                String s = normalize(v == null ? null : v.toString());
                if (s != null) values.add(s);
            }
            return values;
        }
        String s = normalize(val.toString());
        if (s != null) values.add(s);
        return values;
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.toLowerCase(Locale.ROOT);
    }

    private static String stringVal(Object v) {
        return v == null ? null : v.toString();
    }
}

