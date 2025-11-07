package com.example.crp.auth.config;

import com.example.crp.auth.domain.Role;
import com.example.crp.auth.domain.User;
import com.example.crp.auth.repo.RoleRepository;
import com.example.crp.auth.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

@Configuration
public class DataInitializer {
    @Bean
    ApplicationRunner seed(@Value("${ADMIN_EMAIL:admin@crp.local}") String adminEmail,
                           @Value("${ADMIN_PASSWORD:admin}") String adminPassword,
                           RoleRepository roles, UserRepository users, PasswordEncoder encoder) {
        return args -> {
            // ensure roles
            List<String> roleCodes = List.of("ADMIN","MANAGER","ANALYST","USER");
            roleCodes.forEach(code -> roles.findByCode(code).orElseGet(() -> roles.save(newRole(code))));
            // ensure admin user
            users.findByEmail(adminEmail).orElseGet(() -> {
                User u = new User();
                u.setEmail(adminEmail);
                u.setPasswordHash(encoder.encode(adminPassword));
                Set<Role> rs = new java.util.HashSet<>();
                for (String c : roleCodes) { rs.add(roles.findByCode(c).orElseThrow()); }
                u.setRoles(rs);
                return users.save(u);
            });
        };
    }

    private static Role newRole(String code) { Role r = new Role(); r.setCode(code); return r; }
}

