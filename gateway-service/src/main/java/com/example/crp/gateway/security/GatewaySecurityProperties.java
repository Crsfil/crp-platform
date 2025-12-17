package com.example.crp.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {
    private boolean enabled = true;
    private final Mfa mfa = new Mfa();
    private final Abac abac = new Abac();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Mfa getMfa() {
        return mfa;
    }

    public Abac getAbac() {
        return abac;
    }

    public static class Mfa {
        private List<String> paths = new ArrayList<>();
        private String claim = "amr";
        private String value = "otp";
        private boolean adminBypass = true;

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }

        public String getClaim() {
            return claim;
        }

        public void setClaim(String claim) {
            this.claim = claim;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isAdminBypass() {
            return adminBypass;
        }

        public void setAdminBypass(boolean adminBypass) {
            this.adminBypass = adminBypass;
        }
    }

    public static class Abac {
        private List<String> paths = new ArrayList<>();
        private List<String> requiredClaims = List.of("region", "branch");
        private boolean adminBypass = true;

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }

        public List<String> getRequiredClaims() {
            return requiredClaims;
        }

        public void setRequiredClaims(List<String> requiredClaims) {
            this.requiredClaims = requiredClaims;
        }

        public boolean isAdminBypass() {
            return adminBypass;
        }

        public void setAdminBypass(boolean adminBypass) {
            this.adminBypass = adminBypass;
        }
    }
}
