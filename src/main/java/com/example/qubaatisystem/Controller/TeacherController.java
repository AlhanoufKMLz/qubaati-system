package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.TeacherInDTO;
import com.example.qubaatisystem.DTO.Out.ActivityOutDTO;
import com.example.qubaatisystem.DTO.Out.TeacherDashboardClassroomOutDTO;
import com.example.qubaatisystem.DTO.Out.TeacherDashboardStudentOutDTO;
import com.example.qubaatisystem.Enum.ActivityStatus;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.ActivityService;
import com.example.qubaatisystem.Service.TeacherDashboardService;
import com.example.qubaatisystem.Service.TeacherService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final TeacherDashboardService teacherDashboardService;
    private final ActivityService activityService;
    private final SecurityOwnershipService security;

    // Creating/listing teachers is an admin operation (no public self-registration of teachers).
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody TeacherInDTO dto) {
        security.assertAdmin();
        return ResponseEntity.status(200).body(teacherService.create(dto));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        security.assertAdmin();
        return ResponseEntity.status(200).body(teacherService.getAll());
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        security.assertCurrentTeacherOrAdmin(id);
        return ResponseEntity.status(200).body(teacherService.getById(id));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody TeacherInDTO dto) {
        security.assertCurrentTeacherOrAdmin(id);
        return ResponseEntity.status(200).body(teacherService.update(id, dto));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        security.assertCurrentTeacherOrAdmin(id);
        teacherService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("Teacher deleted successfully"));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{teacherId}/dashboard")
    public ResponseEntity<?> getDashboard(@PathVariable Integer teacherId) {
        security.assertCurrentTeacherOrAdmin(teacherId);
        return ResponseEntity.status(200).body(teacherService.getDashboard(teacherId));
    }

    // ---------- teacher ownership / visibility (Student 1) ----------

    // Optional status filter (enum). GET /teachers/{id}/activities?status=DRAFT etc.
    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{teacherId}/activities")
    public ResponseEntity<List<ActivityOutDTO>> getTeacherActivities(
            @PathVariable Integer teacherId,
            @RequestParam(required = false) ActivityStatus status) {
        security.assertCurrentTeacherOrAdmin(teacherId);
        return ResponseEntity.status(200).body(activityService.getActivitiesByTeacher(teacherId, status));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{teacherId}/classrooms")
    public ResponseEntity<List<TeacherDashboardClassroomOutDTO>> getTeacherClassrooms(@PathVariable Integer teacherId) {
        security.assertCurrentTeacherOrAdmin(teacherId);
        return ResponseEntity.status(200).body(teacherDashboardService.getTeacherClassrooms(teacherId));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{teacherId}/students")
    public ResponseEntity<List<TeacherDashboardStudentOutDTO>> getTeacherStudents(@PathVariable Integer teacherId) {
        security.assertCurrentTeacherOrAdmin(teacherId);
        return ResponseEntity.status(200).body(teacherDashboardService.getTeacherStudents(teacherId));
    }

    // ---------- current-teacher ("me") endpoints — no teacherId in the path ----------

    @GetMapping("/me/dashboard")
    public ResponseEntity<?> getMyDashboard() {
        return ResponseEntity.status(200).body(teacherService.getDashboard(security.getCurrentTeacherId()));
    }

    @GetMapping("/me/classrooms")
    public ResponseEntity<List<TeacherDashboardClassroomOutDTO>> getMyClassrooms() {
        return ResponseEntity.status(200).body(teacherDashboardService.getTeacherClassrooms(security.getCurrentTeacherId()));
    }

    @GetMapping("/me/students")
    public ResponseEntity<List<TeacherDashboardStudentOutDTO>> getMyStudents() {
        return ResponseEntity.status(200).body(teacherDashboardService.getTeacherStudents(security.getCurrentTeacherId()));
    }

    @GetMapping("/me/activities")
    public ResponseEntity<List<ActivityOutDTO>> getMyActivities(@RequestParam(required = false) ActivityStatus status) {
        return ResponseEntity.status(200).body(activityService.getActivitiesByTeacher(security.getCurrentTeacherId(), status));
    }
}
