package com.example.crp.inventory.security;

import com.example.crp.inventory.config.InventoryLocationAbacProperties;
import com.example.crp.inventory.domain.Location;
import com.example.crp.security.TrustedClientAuthorizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LocationAccessPolicy {

    private final InventoryLocationAbacProperties props;
    private final TrustedClientAuthorizer trustedClientAuthorizer;

    public LocationAccessPolicy(InventoryLocationAbacProperties props, TrustedClientAuthorizer trustedClientAuthorizer) {
        this.props = props;
        this.trustedClientAuthorizer = trustedClientAuthorizer;
    }

    public void assertWriteAllowed(Authentication auth, Location location) {
        if (!isWriteAllowed(auth, location)) {
            throw new org.springframework.security.access.AccessDeniedException("location_access_denied");
        }
    }

    public void assertReadAllowed(Authentication auth, Location location) {
        if (!isReadAllowed(auth, location)) {
            throw new org.springframework.security.access.AccessDeniedException("location_access_denied");
        }
    }

    public boolean isWriteAllowed(Authentication auth, Location location) {
        if (!props.isEnabled()) {
            return true;
        }
        if (auth == null) {
            return false;
        }
        if (props.isTrustedClientBypass() && trustedClientAuthorizer != null && trustedClientAuthorizer.isTrusted(auth)) {
            return true;
        }
        if (hasRoleAdmin(auth)) {
            return true;
        }
        if (location == null) {
            return false;
        }
        String locRegion = normalize(location.getRegion());
        String locBranch = normalize(location.getCode());
        if (locRegion == null && locBranch == null) {
            return true; // unscoped location
        }
        var userRegions = claimValues(auth, props.getRegionClaim());
        var userBranches = claimValues(auth, props.getBranchClaim());
        if (locRegion != null && userRegions.contains(locRegion)) {
            return true;
        }
        return locBranch != null && userBranches.contains(locBranch);
    }

    public boolean isReadAllowed(Authentication auth, Location location) {
        // for now, same rules as write; can be widened later (e.g., central reporting).
        return isWriteAllowed(auth, location);
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

    private static java.util.Set<String> claimValues(Authentication auth, String claimName) {
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return java.util.Set.of();
        }
        Jwt jwt = jwtAuth.getToken();
        if (jwt == null) return java.util.Set.of();
        Object val = jwt.getClaims().get(claimName);
        if (val == null) return java.util.Set.of();
        java.util.Set<String> values = new java.util.HashSet<>();
        if (val instanceof java.util.Collection<?> collection) {
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
}
