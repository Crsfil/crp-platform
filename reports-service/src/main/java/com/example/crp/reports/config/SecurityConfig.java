package com.example.crp.reports.config;

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
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter filter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .requestMatchers("/reports/**").hasAnyRole("ANALYST","MANAGER","ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public JwtFilter jwtFilter(@Value("${security.jwt.secret}") String secret) { return new JwtFilter(secret); }

    static class JwtFilter extends OncePerRequestFilter {
        private final SecretKey key; JwtFilter(String s){ this.key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(s)); }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            String h = request.getHeader("Authorization");
            if (h!=null && h.startsWith("Bearer ")){
                try {
                    var claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(h.substring(7)).getBody();
                    @SuppressWarnings("unchecked") List<String> roles = (List<String>) claims.get("roles");
                    String email = (String) claims.get("email");
                    var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(email, null,
                            roles.stream().map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+r)).toList());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception ignored) {}
            }
            chain.doFilter(request, response);
        }
    }
}
