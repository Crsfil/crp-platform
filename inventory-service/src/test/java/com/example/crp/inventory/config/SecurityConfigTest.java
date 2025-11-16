package com.example.crp.inventory.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void jwtAuthConverterMapsAuthoritiesAndRoles() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthConverter();

        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of(
                        "authorities", List.of("INVENTORY_WRITE"),
                        "roles", List.of("ADMIN")
                )
        );

        var authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .contains("INVENTORY_WRITE", "ROLE_ADMIN");
    }
}

