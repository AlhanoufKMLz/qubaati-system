package com.example.qubaatisystem.Model;

import com.example.qubaatisystem.Enum.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Application user + Spring Security principal. Basic Auth authenticates against {@code username} / {@code password}
 * (BCrypt-hashed). The single authority is the {@link UserRole} name (no {@code ROLE_} prefix) — matching the
 * reference SecEX project's {@code hasAuthority(...)} style.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    // User receives many Notifications (inverse side)
    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY)
    private Set<Notification> notifications;

    // User is the actor of many AuditLogs (inverse side)
    @OneToMany(mappedBy = "actor", fetch = FetchType.LAZY)
    private Set<AuditLog> auditLogs;

    // ----- Spring Security UserDetails (authority = role name, e.g. "TEACHER"; no ROLE_ prefix) -----

    @Override
    @Transient
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    @Transient
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @Transient
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @Transient
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @Transient
    public boolean isEnabled() {
        return true;
    }
}
