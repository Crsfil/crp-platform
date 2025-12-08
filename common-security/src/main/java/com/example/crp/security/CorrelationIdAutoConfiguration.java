package com.example.crp.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that registers CorrelationIdFilter for servlet-based
 * applications. All services that depend on common-security will automatically
 * get correlation ids in MDC and the X-Correlation-Id response header.
 */
@AutoConfiguration
public class CorrelationIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CorrelationIdFilter.class)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }
}

