package com.example.crp.gateway.bff;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bff")
public class BffProperties {
    private String issuer;
    private String clientId;
    private String scopes = "openid profile email";
    private long proactiveRefreshSkewSeconds = 10;
    private Cookie cookie = new Cookie();

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public long getProactiveRefreshSkewSeconds() { return proactiveRefreshSkewSeconds; }
    public void setProactiveRefreshSkewSeconds(long proactiveRefreshSkewSeconds) { this.proactiveRefreshSkewSeconds = proactiveRefreshSkewSeconds; }
    public Cookie getCookie() { return cookie; }
    public void setCookie(Cookie cookie) { this.cookie = cookie; }

    public static class Cookie {
        private String name = "refresh_token";
        private String domain;
        private boolean secure = false;
        private String sameSite = "Lax"; // Lax/Strict/None

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public boolean isSecure() { return secure; }
        public void setSecure(boolean secure) { this.secure = secure; }
        public String getSameSite() { return sameSite; }
        public void setSameSite(String sameSite) { this.sameSite = sameSite; }
    }
}

