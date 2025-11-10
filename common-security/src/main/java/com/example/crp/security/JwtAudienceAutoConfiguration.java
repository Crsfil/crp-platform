package com.example.crp.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@ConditionalOnClass({JwtDecoder.class, JwtDecoders.class})
@EnableConfigurationProperties(CrpSecurityProperties.class)
public class JwtAudienceAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
    @org.springframework.context.annotation.Primary
    public JwtDecoder jwtDecoder(Environment env, CrpSecurityProperties props,
                                 ObjectProvider<JwtClaimSetConverter> claimSetConverterProvider) {
        String issuer = env.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);

        // Attach validators
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(issuer));

        if (props.isEnforceAudience()) {
            String expected = props.getAudience();
            if (!StringUtils.hasText(expected)) {
                expected = env.getProperty("spring.application.name", "app");
            }
            validators.add(new AudienceValidator(expected));
        }

        ((NimbusJwtDecoder) decoder).setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));

        JwtClaimSetConverter converter = claimSetConverterProvider.getIfAvailable();
        if (converter != null && decoder instanceof NimbusJwtDecoder nimbus) {
            nimbus.setClaimSetConverter(converter);
        }
        return decoder;
    }
}
