package com.example.crp.inventory.config;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtValidatorFilter jwtFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/equipment/**").hasAnyRole("USER","MANAGER","ADMIN","ANALYST")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtValidatorFilter jwtValidatorFilter(@Value("${security.jwt.secret}") String secret,
                                                 @Value("${security.internal-api-key:}") String apiKey) {
        return new JwtValidatorFilter(secret, apiKey);
    }

    static class JwtValidatorFilter extends OncePerRequestFilter {
        private final SecretKey key; private final String apiKey;
        JwtValidatorFilter(String base64Secret, String apiKey) { this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(base64Secret)); this.apiKey=apiKey; }
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
            String apiKeyHdr = request.getHeader("X-Internal-API-Key");
            if (apiKeyHdr != null && !apiKeyHdr.isBlank() && apiKeyHdr.equals(apiKey)) {
                var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "internal", null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANALYST")));
                SecurityContextHolder.getContext().setAuthentication(auth);
                filterChain.doFilter(request, response);
                return;
            }
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    var claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
                    @SuppressWarnings("unchecked") var roles = (List<String>) claims.get("roles");
                    String email = (String) claims.get("email");
                    var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            email, null,
                            roles.stream().map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+r)).toList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception ignored) { }
            }
            filterChain.doFilter(request, response);
        }
    }
}
