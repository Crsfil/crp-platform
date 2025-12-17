package com.example.crp.reports.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "reports.security.abac")
public class ReportsAbacProperties {
    private boolean enabled = true;
    private boolean adminBypass = true;
    private List<String> requiredClaims = new ArrayList<>(List.of("region", "branch"));
    private List<String> paths = new ArrayList<>(List.of(
            "/reports/**",
            "/report-jobs/**",
            "/report-templates/**"
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
