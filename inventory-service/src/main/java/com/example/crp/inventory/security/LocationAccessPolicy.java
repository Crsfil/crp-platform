package com.example.crp.inventory.security;

import com.example.crp.inventory.config.InventoryLocationAbacProperties;
import com.example.crp.inventory.domain.Location;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LocationAccessPolicy {

    private final InventoryLocationAbacProperties props;

    public LocationAccessPolicy(InventoryLocationAbacProperties props) {
        this.props = props;
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
        if (hasRoleAdmin(auth)) {
            return true;
        }
        if (location == null) {
            return false;
        }
        String locRegion = normalize(location.getRegion());
        if (locRegion == null) {
            return true; // unscoped location
        }
        String userRegion = normalize(getClaim(auth, props.getRegionClaim()));
        return userRegion != null && userRegion.equals(locRegion);
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

    private static String getClaim(Authentication auth, String claimName) {
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return null;
        }
        Jwt jwt = jwtAuth.getToken();
        if (jwt == null) return null;
        Object val = jwt.getClaims().get(claimName);
        return val == null ? null : val.toString();
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.toLowerCase(Locale.ROOT);
    }
}
