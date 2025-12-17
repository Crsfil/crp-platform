package com.example.crp.procurement.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "procurement.security.abac")
public class ProcurementAbacProperties {
    private boolean enabled = true;
    private boolean adminBypass = true;
    private boolean trustedClientBypass = true;
    private List<String> requiredClaims = new ArrayList<>(List.of("region", "branch"));
    private List<String> paths = new ArrayList<>(List.of(
            "/requests/**",
            "/service/**",
            "/purchase-orders/**",
            "/goods-receipts/**",
            "/suppliers/**",
            "/attachments/**"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAdminBypass() {
        return adminBypass;
    }

    public void setAdminBypass(boolean adminBypass) {
        this.adminBypass = adminBypass;
    }

    public boolean isTrustedClientBypass() {
        return trustedClientBypass;
    }

    public void setTrustedClientBypass(boolean trustedClientBypass) {
        this.trustedClientBypass = trustedClientBypass;
    }

    public List<String> getRequiredClaims() {
        return requiredClaims;
    }

    public void setRequiredClaims(List<String> requiredClaims) {
        this.requiredClaims = requiredClaims;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }
}
