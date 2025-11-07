package com.example.crp.auth.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.UUID;

@Component
public class KeyProvider {
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId;
    private final String issuer;

    public KeyProvider(@Value("${security.jwt.issuer:http://auth-service:8081}") String issuer) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            this.publicKey = (RSAPublicKey) kp.getPublic();
            this.privateKey = (RSAPrivateKey) kp.getPrivate();
            this.keyId = UUID.randomUUID().toString();
            this.issuer = issuer;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public RSAPublicKey getPublicKey() { return publicKey; }
    public RSAPrivateKey getPrivateKey() { return privateKey; }
    public String getKeyId() { return keyId; }
    public String getIssuer() { return issuer; }

    public Map<String, Object> jwks() {
        RSAKey jwk = new RSAKey.Builder(publicKey).keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .algorithm(new com.nimbusds.jose.Algorithm("RS256")).keyID(keyId).build();
        JWKSet set = new JWKSet(jwk);
        return set.toJSONObject();
    }
}

