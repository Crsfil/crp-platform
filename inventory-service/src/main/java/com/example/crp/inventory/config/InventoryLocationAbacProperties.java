package com.example.crp.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory.security.location-abac")
public class InventoryLocationAbacProperties {
    private boolean enabled = false;
    private String regionClaim = "region";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRegionClaim() {
        return regionClaim;
    }

    public void setRegionClaim(String regionClaim) {
        this.regionClaim = regionClaim;
    }
}

