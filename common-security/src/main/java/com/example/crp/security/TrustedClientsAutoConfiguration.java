package com.example.crp.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Set;

@AutoConfiguration
@ConditionalOnClass(JwtAuthenticationToken.class)
@EnableConfigurationProperties(CrpTrustedClientsProperties.class)
public class TrustedClientsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TrustedClientAuthorizer trustedClientAuthorizer(CrpTrustedClientsProperties props) {
        Set<String> ids = TrustedClientAuthorizer.normalize(Set.copyOf(props.getTrustedClients()));
        return new TrustedClientAuthorizer(ids);
    }
}

