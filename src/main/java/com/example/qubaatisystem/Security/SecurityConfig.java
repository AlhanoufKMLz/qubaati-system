package com.example.qubaatisystem.Security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security — stateless HTTP <b>Basic Auth</b> (no JWT, no session, no cookies), adapted from the SecEX
 * reference project. Authorities are the bare role names ("STUDENT" / "TEACHER" / "PARENT" / "ADMIN") so the
 * matchers use {@code hasAuthority(...)} like SecEX. Every role rule also allows ADMIN.
 *
 * <p>Role rules give the coarse boundary; fine-grained <b>ownership</b> (a parent only sees their own parentId,
 * etc.) is enforced in the controllers via {@code SecurityOwnershipService}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String STUDENT = "STUDENT";
    private static final String TEACHER = "TEACHER";
    private static final String PARENT = "PARENT";
    private static final String ADMIN = "ADMIN";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API: stateless Basic Auth, no CSRF token, no server session.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ---- public (no auth) ----
                        // Local sandbox tester page — serves the HTML form with username/password inputs.
                        .requestMatchers(HttpMethod.GET, "/payment-test.html").permitAll()
                        // Moyasar redirects here after payment with no Qubaati credentials. PaymentService still
                        // verifies the payment with Moyasar before activating anything.
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/callback").permitAll()
                        // Listing plans is harmless public information.
                        .requestMatchers(HttpMethod.GET, "/api/v1/subscriptions/plans").permitAll()

                        // ---- admin-only: technical/generic CRUD + cross-parent batch ----
                        .requestMatchers(HttpMethod.POST, "/api/v1/parents/weekly-reports/generate-all").hasAuthority(ADMIN)
                        .requestMatchers(
                                "/api/v1/audit-logs/**",
                                "/api/v1/options/**",
                                "/api/v1/questions/**",
                                "/api/v1/decisions/**",
                                "/api/v1/insights/**",
                                "/api/v1/learning-styles/**",
                                "/api/v1/learning-style-history/**",
                                "/api/v1/skill-progress-history/**",
                                "/api/v1/student-skills/**",
                                "/api/v1/skills/**",
                                "/api/v1/activity-reviews/**",
                                "/api/v1/mission/**"   // generic mission CRUD + legacy generate (singular path)
                        ).hasAuthority(ADMIN)

                        // ---- parent-scoped ----
                        .requestMatchers("/api/v1/parents/**").hasAnyAuthority(PARENT, ADMIN)
                        .requestMatchers("/api/v1/subscriptions/parents/**").hasAnyAuthority(PARENT, ADMIN)
                        .requestMatchers("/api/v1/ai/parents/**").hasAnyAuthority(PARENT, ADMIN)

                        // ---- teacher-scoped ----
                        .requestMatchers("/api/v1/teachers/**").hasAnyAuthority(TEACHER, ADMIN)
                        .requestMatchers("/api/v1/subscriptions/teachers/**").hasAnyAuthority(TEACHER, ADMIN)
                        .requestMatchers("/api/v1/activities/**").hasAnyAuthority(TEACHER, ADMIN)
                        .requestMatchers("/api/v1/classrooms/**").hasAnyAuthority(TEACHER, ADMIN)
                        .requestMatchers("/api/v1/ai/teachers/**").hasAnyAuthority(TEACHER, ADMIN)
                        .requestMatchers("/api/v1/ai/classrooms/**").hasAnyAuthority(TEACHER, ADMIN)
                        .requestMatchers("/api/v1/ai/activities/**").hasAnyAuthority(TEACHER, ADMIN)
                        .requestMatchers("/api/v1/ai/activity-submissions/**").hasAnyAuthority(TEACHER, ADMIN)

                        // ---- student-scoped ----
                        .requestMatchers("/api/v1/students/**").hasAnyAuthority(STUDENT, ADMIN)
                        .requestMatchers("/api/v1/mission-sessions/**").hasAnyAuthority(STUDENT, ADMIN)

                        // ---- shared, authenticated (any logged-in role; finer ownership in services) ----
                        // AI health, payments checkout/status/receipt, mission flow authoring/play, career worlds,
                        // submissions, assignments, notifications, recommendations, user notifications, etc.
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.authenticationEntryPoint(
                        (request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            // No WWW-Authenticate header — prevents the browser-native Basic Auth popup.
                            response.getWriter().write("{\"message\":\"Unauthorized\"}");
                        }
                ));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
