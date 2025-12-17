package com.example.crp.gateway.security;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFiltersOrder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@Order(SecurityWebFiltersOrder.AUTHORIZATION.getOrder() + 1)
public class GatewayAccessPolicyFilter implements WebFilter {
    private final GatewaySecurityProperties props;
    private final List<PathPattern> mfaPatterns;
    private final List<PathPattern> abacPatterns;

    public GatewayAccessPolicyFilter(GatewaySecurityProperties props) {
        this.props = props;
        PathPatternParser parser = new PathPatternParser();
        this.mfaPatterns = compile(parser, props.getMfa().getPaths());
        this.abacPatterns = compile(parser, props.getAbac().getPaths());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!props.isEnabled()) {
            return chain.filter(exchange);
        }
        var path = exchange.getRequest().getPath().pathWithinApplication();
        boolean requireMfa = matches(mfaPatterns, path);
        boolean requireAbac = matches(abacPatterns, path);
        if (!requireMfa && !requireAbac) {
            return chain.filter(exchange);
        }
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> enforce(exchange, chain, auth, requireMfa, requireAbac))
                .switchIfEmpty(forbidden(exchange));
    }

    private Mono<Void> enforce(ServerWebExchange exchange, WebFilterChain chain, Authentication auth,
                               boolean requireMfa, boolean requireAbac) {
        if (auth == null || !auth.isAuthenticated()) {
            return forbidden(exchange);
        }
        if (requireMfa && !mfaSatisfied(auth)) {
            return forbidden(exchange);
        }
        if (requireAbac && !abacSatisfied(auth)) {
            return forbidden(exchange);
        }
        return chain.filter(exchange);
    }

    private boolean mfaSatisfied(Authentication auth) {
        if (props.getMfa().isAdminBypass() && hasRoleAdmin(auth)) {
            return true;
        }
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return false;
        }
        Jwt jwt = jwtAuth.getToken();
        if (jwt == null) {
            return false;
        }
        Object claimValue = jwt.getClaims().get(props.getMfa().getClaim());
        return matchesClaim(claimValue, props.getMfa().getValue());
    }

    private boolean abacSatisfied(Authentication auth) {
        if (props.getAbac().isAdminBypass() && hasRoleAdmin(auth)) {
            return true;
        }
        List<String> required = props.getAbac().getRequiredClaims();
        if (required == null || required.isEmpty()) {
            return true;
        }
        for (String claim : required) {
            if (claim == null || claim.isBlank()) {
                continue;
            }
            Set<String> values = claimValues(auth, claim);
            if (values.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static List<PathPattern> compile(PathPatternParser parser, List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        return raw.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(p -> parser.parse(p.trim()))
                .toList();
    }

    private static boolean matches(List<PathPattern> patterns, org.springframework.http.server.PathContainer path) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (PathPattern pattern : patterns) {
            if (pattern.matches(path)) {
                return true;
            }
        }
        return false;
    }

    private static Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
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

    private static Set<String> claimValues(Authentication auth, String claimName) {
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return Set.of();
        }
        Jwt jwt = jwtAuth.getToken();
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
