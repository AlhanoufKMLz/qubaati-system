package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.StudentInDTO;
import com.example.qubaatisystem.DTO.Out.StudentActivityDashboardOutDTO;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.ActivityDashboardService;
import com.example.qubaatisystem.Service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final ActivityDashboardService activityDashboardService;
    private final SecurityOwnershipService security;

    // ---------- CRUD ----------

    // Students are created by an admin (or by a parent via POST /parents/{parentId}/children).
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody StudentInDTO dto) {
        security.assertAdmin();
        return ResponseEntity.status(200).body(studentService.create(dto));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        security.assertAdmin();
        return ResponseEntity.status(200).body(studentService.getAll());
    }

    // Current student's own profile — no studentId in the path.
    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        return ResponseEntity.status(200).body(studentService.getById(security.getCurrentStudentId()));
    }

    @Deprecated // prefer GET /students/me
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        security.assertCurrentStudentOrAdmin(id);
        return ResponseEntity.status(200).body(studentService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody StudentInDTO dto) {
        security.assertCurrentStudentOrAdmin(id);
        return ResponseEntity.status(200).body(studentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        security.assertCurrentStudentOrAdmin(id);
        studentService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("Student deleted successfully"));
    }

    // ---------- AVAILABILITY / HISTORY ----------

    @GetMapping("/{studentId}/career-worlds/available")
    public ResponseEntity<?> getAvailableCareerWorlds(@PathVariable Integer studentId) {
        security.assertCurrentStudentOrAdmin(studentId);
        return ResponseEntity.status(200).body(studentService.getAvailableCareerWorlds(studentId));
    }

    @GetMapping("/me/skills/history")
    public ResponseEntity<?> getMySkillHistory() {
        return ResponseEntity.status(200).body(studentService.getSkillHistory(security.getCurrentStudentId()));
    }

    @GetMapping("/me/learning-style/history")
    public ResponseEntity<?> getMyLearningStyleHistory() {
        return ResponseEntity.status(200).body(studentService.getLearningStyleHistory(security.getCurrentStudentId()));
    }

    @Deprecated // prefer GET /students/me/skills/history
    @GetMapping("/{studentId}/skills/history")
    public ResponseEntity<?> getSkillHistory(@PathVariable Integer studentId) {
        security.assertCurrentStudentOrAdmin(studentId);
        return ResponseEntity.status(200).body(studentService.getSkillHistory(studentId));
    }

    @GetMapping("/{studentId}/learning-style/history")
    public ResponseEntity<?> getLearningStyleHistory(@PathVariable Integer studentId) {
        security.assertCurrentStudentOrAdmin(studentId);
        return ResponseEntity.status(200).body(studentService.getLearningStyleHistory(studentId));
    }

    // ---------- ACTIVITY DASHBOARD (Student 2) ----------

    @GetMapping("/{studentId}/activity-dashboard")
    public ResponseEntity<StudentActivityDashboardOutDTO> getActivityDashboard(@PathVariable Integer studentId) {
        security.assertCurrentStudentOrAdmin(studentId);
        return ResponseEntity.status(200).body(activityDashboardService.getStudentActivityDashboard(studentId));
    }

    // ---------- current-student ("me") endpoints — no studentId in the path ----------

    @GetMapping("/me/activity-dashboard")
    public ResponseEntity<StudentActivityDashboardOutDTO> getMyActivityDashboard() {
        return ResponseEntity.status(200)
                .body(activityDashboardService.getStudentActivityDashboard(security.getCurrentStudentId()));
    }

    @GetMapping("/me/career-worlds/available")
    public ResponseEntity<?> getMyAvailableCareerWorlds() {
        return ResponseEntity.status(200).body(studentService.getAvailableCareerWorlds(security.getCurrentStudentId()));
    }
}
