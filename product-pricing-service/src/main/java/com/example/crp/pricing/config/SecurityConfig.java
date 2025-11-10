package com.example.crp.pricing.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean public SecurityFilterChain filterChain(HttpSecurity http, ObjectProvider<InternalApiKeyFilter> internal) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a->a.requestMatchers("/actuator/**","/v3/api-docs/**","/swagger-ui.html","/swagger-ui/**").permitAll().anyRequest().authenticated())
                ;
        InternalApiKeyFilter f = internal.getIfAvailable(); if (f != null) { http.addFilterBefore(f, UsernamePasswordAuthenticationFilter.class);} 
        http.oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
        return http.build(); }

    @Bean public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("authorities");
        gac.setAuthorityPrefix("");
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt -> { var a = gac.convert(jwt); a.addAll(roles.convert(jwt)); return a; });
        return conv;
    }

    @Bean
    @ConditionalOnProperty(prefix = "security.internal-api-key", name = "enabled", havingValue = "true")
    public InternalApiKeyFilter internalApiKeyFilter(@Value("${security.internal-api-key:}") String apiKey) { return new InternalApiKeyFilter(apiKey, "ROLE_ANALYST"); }

    static class InternalApiKeyFilter extends OncePerRequestFilter{ private final String apiKey; private final String role; InternalApiKeyFilter(String apiKey,String role){ this.apiKey=apiKey; this.role=role; }
        @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException { String api=req.getHeader("X-Internal-API-Key"); if(api!=null && !api.isEmpty() && api.equals(apiKey)){ var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("internal",null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(role))); org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);} chain.doFilter(req,res);} }
}
