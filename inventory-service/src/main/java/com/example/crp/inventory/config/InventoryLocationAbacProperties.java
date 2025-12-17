package com.example.crp.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory.security.location-abac")
public class InventoryLocationAbacProperties {
    private boolean enabled = false;
    private boolean trustedClientBypass = true;
    private String regionClaim = "region";
    private String branchClaim = "branch";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isTrustedClientBypass() {
        return trustedClientBypass;
    }

    public void setTrustedClientBypass(boolean trustedClientBypass) {
        this.trustedClientBypass = trustedClientBypass;
    }

    public String getRegionClaim() {
        return regionClaim;
    }

    public void setRegionClaim(String regionClaim) {
        this.regionClaim = regionClaim;
    }

    public String getBranchClaim() {
        return branchClaim;
    }

    public void setBranchClaim(String branchClaim) {
        this.branchClaim = branchClaim;
    }
}

