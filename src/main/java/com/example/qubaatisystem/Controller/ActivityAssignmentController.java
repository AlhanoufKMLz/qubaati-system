package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.ActivityAssignInDTO;
import com.example.qubaatisystem.DTO.In.ActivityAssignmentBulkInDTO;
import com.example.qubaatisystem.DTO.In.ActivityAssignmentDeadlineInDTO;
import com.example.qubaatisystem.DTO.In.ActivityAssignmentInDTO;
import com.example.qubaatisystem.DTO.Out.ActivityAssignmentOutDTO;
import com.example.qubaatisystem.Service.ActivityAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ActivityAssignmentController {

    private final ActivityAssignmentService activityAssignmentService;

    // ---------- CRUD ----------

    @PostMapping("/activity-assignments")
    public ResponseEntity<?> create(@Valid @RequestBody ActivityAssignmentInDTO dto) {
        activityAssignmentService.create(dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityAssignment created successfully"));
    }

    @GetMapping("/activity-assignments")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(200).body(activityAssignmentService.getAll());
    }

    @GetMapping("/activity-assignments/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(activityAssignmentService.getById(id));
    }

    @PutMapping("/activity-assignments/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody ActivityAssignmentInDTO dto) {
        activityAssignmentService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityAssignment updated successfully"));
    }

    @DeleteMapping("/activity-assignments/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        activityAssignmentService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityAssignment deleted successfully"));
    }

    // ---------- FLOW: ASSIGNMENT ----------

    @PostMapping("/activities/{activityId}/assign/students/{studentId}")
    public ResponseEntity<ActivityAssignmentOutDTO> assignToStudent(
            @PathVariable Integer activityId,
            @PathVariable Integer studentId,
            @Valid @RequestBody ActivityAssignInDTO dto) {
        return ResponseEntity.status(200).body(activityAssignmentService.assignToStudent(activityId, studentId, dto));
    }

    @PostMapping("/activities/{activityId}/assign/classrooms/{classroomId}")
    public ResponseEntity<ActivityAssignmentOutDTO> assignToClassroom(
            @PathVariable Integer activityId,
            @PathVariable Integer classroomId,
            @Valid @RequestBody ActivityAssignInDTO dto) {
        return ResponseEntity.status(200).body(activityAssignmentService.assignToClassroom(activityId, classroomId, dto));
    }

    @PostMapping("/activities/{activityId}/assign/bulk-students")
    public ResponseEntity<ApiResponse> assignToBulkStudents(
            @PathVariable Integer activityId,
            @Valid @RequestBody ActivityAssignmentBulkInDTO dto) {
        return ResponseEntity.status(200).body(activityAssignmentService.assignToBulkStudents(activityId, dto));
    }

    @GetMapping("/activities/{activityId}/assignments")
    public ResponseEntity<List<ActivityAssignmentOutDTO>> getAssignmentsByActivity(@PathVariable Integer activityId) {
        return ResponseEntity.status(200).body(activityAssignmentService.getAssignmentsByActivity(activityId));
    }

    @GetMapping("/students/{studentId}/activity-assignments")
    public ResponseEntity<List<ActivityAssignmentOutDTO>> getAssignmentsByStudent(@PathVariable Integer studentId) {
        return ResponseEntity.status(200).body(activityAssignmentService.getAssignmentsByStudent(studentId));
    }

    @PatchMapping("/activity-assignments/{assignmentId}/cancel")
    public ResponseEntity<ApiResponse> cancelAssignment(@PathVariable Integer assignmentId) {
        activityAssignmentService.cancelAssignment(assignmentId);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityAssignment cancelled successfully"));
    }

    @PatchMapping("/activity-assignments/{assignmentId}/extend-deadline")
    public ResponseEntity<ApiResponse> extendDeadline(
            @PathVariable Integer assignmentId,
            @Valid @RequestBody ActivityAssignmentDeadlineInDTO dto) {
        activityAssignmentService.extendDeadline(assignmentId, dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityAssignment deadline extended successfully"));
    }
}
