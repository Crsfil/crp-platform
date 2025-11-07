package com.example.crp.auth.service;

import com.example.crp.auth.config.KeyProvider;
import com.example.crp.auth.domain.Permission;
import com.example.crp.auth.domain.Role;
import com.example.crp.auth.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JwtService {
    private final RSAPrivateKey privateKey;
    private final String keyId;
    private final String issuer;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtService(KeyProvider keys,
                      @Value("${security.jwt.access-ttl-s:1800}") long accessTtlSeconds,
                      @Value("${security.jwt.refresh-ttl-s:1209600}") long refreshTtlSeconds) {
        this.privateKey = keys.getPrivateKey();
        this.keyId = keys.getKeyId();
        this.issuer = keys.getIssuer();
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(Role::getCode).toList();
        Set<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream()).map(Permission::getCode)
                .collect(Collectors.toSet());
        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setIssuer(issuer)
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim("authorities", permissions)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String generateRefreshToken(User user, String tokenId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("kid", keyId)
                .setIssuer(issuer)
                .setSubject(user.getId().toString())
                .claim("typ", "refresh")
                .claim("tid", tokenId)
                .claim("email", user.getEmail())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public long getAccessTtlSeconds() { return accessTtlSeconds; }
    public long getRefreshTtlSeconds() { return refreshTtlSeconds; }
}
