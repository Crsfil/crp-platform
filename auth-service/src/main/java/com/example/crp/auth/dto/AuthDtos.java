package com.example.crp.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public static class RegisterRequest {
        @Email public String email;
        @NotBlank public String password;
    }
    public static class LoginRequest {
        @Email public String email;
        @NotBlank public String password;
    }
    public static class RefreshRequest {
        @NotBlank public String refreshToken;
    }
    public static class TokenResponse {
        public String accessToken;
        public String refreshToken;
        public String tokenType = "Bearer";
        public long expiresIn;
        public TokenResponse(String at, String rt, long exp) {
            this.accessToken = at; this.refreshToken = rt; this.expiresIn = exp;
        }
    }
}

