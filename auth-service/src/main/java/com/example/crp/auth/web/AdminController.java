package com.example.crp.auth.web;

import com.example.crp.auth.domain.Role;
import com.example.crp.auth.domain.User;
import com.example.crp.auth.repo.RoleRepository;
import com.example.crp.auth.repo.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final UserRepository users;
    private final RoleRepository roles;

    public AdminController(UserRepository users, RoleRepository roles) { this.users = users; this.roles = roles; }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> listUsers() { return users.findAll(); }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Role> listRoles() { return roles.findAll(); }

    @PostMapping("/users/{id}/roles/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addRole(@PathVariable Long id, @PathVariable String code) {
        User u = users.findById(id).orElseThrow();
        Role r = roles.findByCode(code).orElseThrow();
        u.getRoles().add(r);
        users.save(u);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}/roles/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeRole(@PathVariable Long id, @PathVariable String code) {
        User u = users.findById(id).orElseThrow();
        roles.findByCode(code).ifPresent(u.getRoles()::remove);
        users.save(u);
        return ResponseEntity.noContent().build();
    }
}

