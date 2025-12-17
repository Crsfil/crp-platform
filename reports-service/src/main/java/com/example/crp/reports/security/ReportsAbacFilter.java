package com.example.crp.reports.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ReportsAbacFilter extends OncePerRequestFilter {
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    );

    private final ReportsAbacProperties props;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public ReportsAbacFilter(ReportsAbacProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        if (matches(EXCLUDED_PATHS, path)) {
            return true;
        }
        return !matches(props.getPaths(), path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }
        if (props.isAdminBypass() && hasRoleAdmin(auth)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }
        Jwt jwt = jwtAuth.getToken();
        if (jwt == null) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }
        if (!hasRequiredClaims(jwt, props.getRequiredClaims())) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean matches(List<String> patterns, String path) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (pattern != null && matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRequiredClaims(Jwt jwt, List<String> requiredClaims) {
        if (requiredClaims == null || requiredClaims.isEmpty()) {
            return true;
        }
        for (String claim : requiredClaims) {
            if (claim == null || claim.isBlank()) {
                continue;
            }
            Set<String> values = claimValues(jwt, claim);
            if (values.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> claimValues(Jwt jwt, String claimName) {
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

    private static boolean hasRoleAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(ga.getAuthority()) || "ADMIN".equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.toLowerCase(Locale.ROOT);
    }
}
