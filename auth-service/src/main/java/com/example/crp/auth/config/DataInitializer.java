package com.example.crp.auth.config;

import com.example.crp.auth.domain.Permission;
import com.example.crp.auth.domain.Role;
import com.example.crp.auth.domain.User;
import com.example.crp.auth.repo.PermissionRepository;
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
                           RoleRepository roles, PermissionRepository perms, UserRepository users, PasswordEncoder encoder) {
        return args -> {
            // ensure roles
            List<String> roleCodes = List.of("ADMIN","MANAGER","ANALYST","USER");
            roleCodes.forEach(code -> roles.findByCode(code).orElseGet(() -> roles.save(newRole(code))));
            // ensure permissions
            List<String> permCodes = List.of("PROCUREMENT_APPROVE","INVENTORY_WRITE","REPORTS_READ");
            permCodes.forEach(code -> perms.findByCode(code).orElseGet(() -> { Permission p = new Permission(); p.setCode(code); return perms.save(p); }));
            // map permissions to roles
            Role admin = roles.findByCode("ADMIN").orElseThrow();
            admin.getPermissions().addAll(perms.findAll());
            roles.save(admin);
            Role manager = roles.findByCode("MANAGER").orElseThrow();
            manager.getPermissions().add(perms.findByCode("PROCUREMENT_APPROVE").orElseThrow());
            manager.getPermissions().add(perms.findByCode("INVENTORY_WRITE").orElseThrow());
            manager.getPermissions().add(perms.findByCode("REPORTS_READ").orElseThrow());
            roles.save(manager);
            Role analyst = roles.findByCode("ANALYST").orElseThrow();
            analyst.getPermissions().add(perms.findByCode("REPORTS_READ").orElseThrow());
            roles.save(analyst);
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
