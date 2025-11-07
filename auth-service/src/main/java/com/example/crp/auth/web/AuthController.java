package com.example.crp.auth.web;

import com.example.crp.auth.config.KeyProvider;
import com.example.crp.auth.domain.User;
import com.example.crp.auth.dto.AuthDtos.LoginRequest;
import com.example.crp.auth.dto.AuthDtos.RefreshRequest;
import com.example.crp.auth.dto.AuthDtos.RegisterRequest;
import com.example.crp.auth.dto.AuthDtos.TokenResponse;
import com.example.crp.auth.service.JwtService;
import com.example.crp.auth.service.RedisTokenStore;
import com.example.crp.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final RedisTokenStore tokenStore;
    private final KeyProvider keys;

    public AuthController(AuthenticationManager authManager, UserService userService, JwtService jwtService, RedisTokenStore tokenStore, KeyProvider keys) {
        this.authManager = authManager; this.userService = userService; this.jwtService = jwtService; this.tokenStore = tokenStore; this.keys = keys;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        User u = userService.register(req.email, req.password, List.of("USER"));
        return ResponseEntity.ok(Map.of("id", u.getId(), "email", u.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(req.email, req.password));
        SecurityContextHolder.getContext().setAuthentication(auth);
        User u = userService.findByEmail(req.email);
        String access = jwtService.generateAccessToken(u);
        String tid = UUID.randomUUID().toString();
        String refresh = jwtService.generateRefreshToken(u, tid);
        tokenStore.storeRefresh(tid, jwtService.getRefreshTtlSeconds());
        return ResponseEntity.ok(new TokenResponse(access, refresh, jwtService.getAccessTtlSeconds()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest req) {
        try {
            var claims = io.jsonwebtoken.Jwts.parserBuilder().setSigningKey(keys.getPublicKey()).build().parseClaimsJws(req.refreshToken).getBody();
            if (!"refresh".equals(claims.get("typ"))) return ResponseEntity.badRequest().body(Map.of("error","invalid token type"));
            String tid = (String) claims.get("tid");
            if (!tokenStore.isRefreshActive(tid)) return ResponseEntity.status(401).body(Map.of("error","refresh revoked"));
            Long userId = Long.valueOf(claims.getSubject());
            User u;
            Object emailClaim = claims.get("email");
            if (emailClaim != null) u = userService.findByEmail(emailClaim.toString()); else u = userService.findById(userId);
            String access = jwtService.generateAccessToken(u);
            return ResponseEntity.ok(new TokenResponse(access, req.refreshToken, jwtService.getAccessTtlSeconds()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error","invalid token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String,String> body) {
        String refresh = body.get("refreshToken");
        if (refresh != null) {
            try {
                var claims = Jwts.parserBuilder().setSigningKey((SecretKey) jwtService.getKey()).build().parseClaimsJws(refresh).getBody();
                String tid = (String) claims.get("tid");
                tokenStore.revokeRefresh(tid);
            } catch (Exception ignored) { }
        }
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}
