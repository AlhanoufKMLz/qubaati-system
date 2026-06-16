package com.example.qubaatisystem.Model;

import com.example.qubaatisystem.Enum.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

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
}
