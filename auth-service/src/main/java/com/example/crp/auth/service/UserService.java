package com.example.crp.auth.service;

import com.example.crp.auth.domain.Role;
import com.example.crp.auth.domain.User;
import com.example.crp.auth.repo.RoleRepository;
import com.example.crp.auth.repo.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users; this.roles = roles; this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = users.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("not found"));
        return new org.springframework.security.core.userdetails.User(
                u.getEmail(), u.getPasswordHash(),
                u.getRoles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.getCode())).toList()
        );
    }

    @Transactional
    public User register(String email, String rawPassword, List<String> roleCodes) {
        if (users.existsByEmail(email)) throw new IllegalArgumentException("email already used");
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setCreatedAt(OffsetDateTime.now());
        Set<Role> r = roleCodes.stream().map(code -> roles.findByCode(code).orElseGet(() -> {
            Role nr = new Role(); nr.setCode(code); return roles.save(nr);
        })).collect(java.util.stream.Collectors.toSet());
        u.setRoles(r);
        return users.save(u);
    }

    public User findByEmail(String email) { return users.findByEmail(email).orElseThrow(); }
    public User findById(Long id) { return users.findById(id).orElseThrow(); }
}
