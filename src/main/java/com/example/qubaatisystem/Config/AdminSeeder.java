package com.example.qubaatisystem.Config;

import com.example.qubaatisystem.Enum.UserRole;
import com.example.qubaatisystem.Model.User;
import com.example.qubaatisystem.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a single ADMIN user on startup if one with the configured username does not already exist. Provides the
 * bootstrap account used to create teachers/parents/students (which are admin-only CRUD) and to reach the generic
 * admin endpoints. The password is BCrypt-hashed and never logged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:Admin123!}")
    private String adminPassword;

    @Value("${app.admin.email:admin@qubaati.test}")
    private String adminEmail;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findUserByUsername(adminUsername) != null) {
            return;
        }
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
        log.info("Seeded ADMIN user '{}' (password from app.admin.password).", adminUsername);
    }
}
