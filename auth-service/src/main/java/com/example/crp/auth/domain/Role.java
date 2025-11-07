package com.example.crp.auth.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code; // ADMIN, MANAGER, ANALYST, USER

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private java.util.Set<Permission> permissions = new java.util.HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public java.util.Set<Permission> getPermissions() { return permissions; }
    public void setPermissions(java.util.Set<Permission> permissions) { this.permissions = permissions; }
}
