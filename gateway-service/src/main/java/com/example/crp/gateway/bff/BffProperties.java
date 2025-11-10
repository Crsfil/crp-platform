package com.example.crp.gateway.bff;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bff")
public class BffProperties {
    private String issuer = "http://keycloak:8080/realms/crp";
    private String clientId = "crp-cli";
    private String scopes = "openid profile email offline_access";
    private Cookie cookie = new Cookie();
    private long proactiveRefreshSkewSeconds = 120; // refresh if < 120s

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public Cookie getCookie() { return cookie; }
    public void setCookie(Cookie cookie) { this.cookie = cookie; }
    public long getProactiveRefreshSkewSeconds() { return proactiveRefreshSkewSeconds; }
    public void setProactiveRefreshSkewSeconds(long v) { this.proactiveRefreshSkewSeconds = v; }

    public static class Cookie {
        private String domain = null; // default host
        private boolean secure = false; // enable true in prod
        private String sameSite = "Lax"; // Lax/Strict/None
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }
        public String getSameSite() { return sameSite; }
        public void setSameSite(String sameSite) { this.sameSite = sameSite; }
    }
}

