package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SecurityOwnershipService security;

    // Public — listing plans is harmless (also permitAll in SecurityConfig).
    @GetMapping("/plans")
    public ResponseEntity<?> getActivePlans() {
        return ResponseEntity.ok(subscriptionService.getActivePlans());
    }

    // Current-user subscription status — no profile id in the path.
    @GetMapping("/parents/me/status")
    public ResponseEntity<?> getMyParentStatus() {
        return ResponseEntity.ok(subscriptionService.getParentStatus(security.getCurrentParentId()));
    }

    @GetMapping("/teachers/me/status")
    public ResponseEntity<?> getMyTeacherStatus() {
        return ResponseEntity.ok(subscriptionService.getTeacherStatus(security.getCurrentTeacherId()));
    }

    @Deprecated // prefer GET /subscriptions/parents/me/status
    @GetMapping("/parents/{parentId}/status")
    public ResponseEntity<?> getParentStatus(@PathVariable Integer parentId) {
        security.assertCurrentParentOrAdmin(parentId);
        return ResponseEntity.ok(subscriptionService.getParentStatus(parentId));
    }

    @Deprecated // prefer GET /subscriptions/teachers/me/status
    @GetMapping("/teachers/{teacherId}/status")
    public ResponseEntity<?> getTeacherStatus(@PathVariable Integer teacherId) {
        security.assertCurrentTeacherOrAdmin(teacherId);
        return ResponseEntity.ok(subscriptionService.getTeacherStatus(teacherId));
    }
}
