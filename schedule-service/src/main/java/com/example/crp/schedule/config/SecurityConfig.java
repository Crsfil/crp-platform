package com.example.crp.schedule.config;

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
    @Bean public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter filter) throws Exception { http.csrf(csrf->csrf.disable()).authorizeHttpRequests(a->a.requestMatchers("/actuator/**","/v3/api-docs/**","/swagger-ui.html","/swagger-ui/**").permitAll().anyRequest().authenticated()).addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class); return http.build(); }
    @Bean public JwtFilter jwtFilter(@Value("${security.jwt.secret}") String s, @Value("${security.internal-api-key:}") String k){ return new JwtFilter(s,k);}    
    static class JwtFilter extends OncePerRequestFilter{ private final SecretKey key; private final String apiKey; JwtFilter(String s,String apiKey){ this.key=io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(s)); this.apiKey=apiKey; }
        @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException { String api=req.getHeader("X-Internal-API-Key"); if(api!=null && api.equals(apiKey)){ var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("internal",null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANALYST"))); SecurityContextHolder.getContext().setAuthentication(auth); chain.doFilter(req,res); return; } String h=req.getHeader("Authorization"); if(h!=null && h.startsWith("Bearer ")){ try{ var c=Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(h.substring(7)).getBody(); @SuppressWarnings("unchecked") List<String> r=(List<String>)c.get("roles"); String e=(String)c.get("email"); var a=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(e,null,r.stream().map(x->new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+x)).toList()); SecurityContextHolder.getContext().setAuthentication(a);}catch(Exception ignored){} } chain.doFilter(req,res);} }
}

