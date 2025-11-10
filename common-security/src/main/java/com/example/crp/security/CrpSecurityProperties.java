package com.example.crp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crp.security.jwt")
public class CrpSecurityProperties {
    /**
     * Expected audience for resource services. If null/blank, falls back to 'spring.application.name'.
     */
    private String audience;

    /**
     * Enable audience enforcement. Default true.
     */
    private boolean enforceAudience = false;

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public boolean isEnforceAudience() {
        return enforceAudience;
    }

    public void setEnforceAudience(boolean enforceAudience) {
        this.enforceAudience = enforceAudience;
    }
}
