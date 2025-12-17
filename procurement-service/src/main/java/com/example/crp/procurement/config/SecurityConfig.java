package com.example.crp.procurement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.crp.procurement.security.ProcurementAbacFilter;
import com.example.crp.procurement.security.ProcurementAbacProperties;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(ProcurementAbacProperties.class)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ProcurementAbacFilter abacFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                        // Создание заявок — любая аутентификация; утверждение/отклонение — на методах через @PreAuthorize
                        .requestMatchers(HttpMethod.POST, "/requests/**").authenticated()
                        .requestMatchers("/requests/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
                .addFilterAfter(abacFilter, BearerTokenAuthenticationFilter.class);
        return http.build();
    }
    @Bean
    public JwtAuthenticationConverter jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter gac = new JwtGrantedAuthoritiesConverter();
        gac.setAuthoritiesClaimName("authorities");
        gac.setAuthorityPrefix("");
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = gac.convert(jwt);
            var rolesConv = new JwtGrantedAuthoritiesConverter();
            rolesConv.setAuthoritiesClaimName("roles");
            rolesConv.setAuthorityPrefix("ROLE_");
            authorities.addAll(rolesConv.convert(jwt));
            return authorities;
        });
        return conv;
    }
}
