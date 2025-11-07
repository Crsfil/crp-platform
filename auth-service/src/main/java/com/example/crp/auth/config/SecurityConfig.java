package com.example.crp.auth.config;

import io.jsonwebtoken.Jwts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationManager authenticationManager(org.springframework.security.core.userdetails.UserDetailsService uds,
                                                       PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(List.of(provider));
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, KeyProvider keys) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
                                "/auth/login", "/auth/register", "/auth/refresh", "/.well-known/jwks.json").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(keys.getPublicKey()), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    static class JwtAuthFilter extends OncePerRequestFilter {
        private final RSAPublicKey publicKey;
        JwtAuthFilter(RSAPublicKey publicKey) { this.publicKey = publicKey; }
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                try {
                    var claims = Jwts.parserBuilder().setSigningKey(publicKey).build()
                            .parseClaimsJws(token).getBody();
                    String email = (String) claims.get("email");
                    @SuppressWarnings("unchecked") var roles = (List<String>) claims.get("roles");
                    var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            email, null,
                            roles.stream().map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+r)).toList()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception ignored) { }
            }
            filterChain.doFilter(request, response);
        }
    }
}
