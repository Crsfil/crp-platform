package com.example.crp.inventory.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Locale;

@Component("mfaPolicy")
public class MfaPolicy {

    private final boolean required;
    private final String claim;
    private final String value;

    public MfaPolicy(@Value("${crp.security.mfa.required:false}") boolean required,
                     @Value("${crp.security.mfa.claim:amr}") String claim,
                     @Value("${crp.security.mfa.value:otp}") String value) {
        this.required = required;
        this.claim = claim;
        this.value = value;
    }

    public boolean isSatisfied(Authentication auth) {
        if (!required) {
            return true;
        }
        if (auth == null) {
            return false;
        }
        if (hasRoleAdmin(auth)) {
            return true;
        }
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return false;
        }
        Jwt jwt = jwtAuth.getToken();
        if (jwt == null) {
            return false;
        }
        Object claimValue = jwt.getClaims().get(claim);
        return matchesClaim(claimValue, value);
    }

    private static boolean hasRoleAdmin(Authentication auth) {
        if (auth.getAuthorities() == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(ga.getAuthority()) || "ADMIN".equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesClaim(Object claimValue, String expected) {
        if (claimValue == null) return false;
        if (claimValue instanceof Collection<?> collection) {
            for (Object v : collection) {
                if (matchesClaim(v, expected)) return true;
            }
            return false;
        }
        String expectedNorm = normalize(expected);
        String actual = normalize(claimValue.toString());
        if (expectedNorm == null || actual == null) return false;
        if (isNumeric(expectedNorm) && isNumeric(actual)) {
            try {
                return Double.parseDouble(actual) >= Double.parseDouble(expectedNorm);
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return expectedNorm.equals(actual);
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isBlank()) return false;
        return s.chars().allMatch(ch -> Character.isDigit(ch) || ch == '.');
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.toLowerCase(Locale.ROOT);
    }
}
