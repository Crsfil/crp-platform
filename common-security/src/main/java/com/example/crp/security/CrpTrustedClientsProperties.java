package com.example.crp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "crp.security")
public class CrpTrustedClientsProperties {

    /**
     * List of OIDC clientIds that are allowed to bypass method RBAC for read-only internal endpoints.
     * Values are matched against JWT claims like 'azp' (Keycloak) and 'client_id'.
     */
    private List<String> trustedClients = new ArrayList<>();

    public List<String> getTrustedClients() {
        return trustedClients;
    }

    public void setTrustedClients(List<String> trustedClients) {
        this.trustedClients = trustedClients;
    }
}

