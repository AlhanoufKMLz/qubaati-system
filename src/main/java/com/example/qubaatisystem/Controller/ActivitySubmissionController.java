package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.ActivitySubmissionInDTO;
import com.example.qubaatisystem.DTO.In.ActivitySubmissionReturnInDTO;
import com.example.qubaatisystem.DTO.In.TeacherFeedbackInDTO;
import com.example.qubaatisystem.DTO.Out.ActivitySubmissionOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentActivityAttemptOutDTO;
import com.example.qubaatisystem.Service.ActivitySubmissionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ActivitySubmissionController {

    private final ActivitySubmissionService activitySubmissionService;

    // ---------- CRUD ----------

    @PostMapping("/activity-submissions")
    public ResponseEntity<?> create(@Valid @RequestBody ActivitySubmissionInDTO dto) {
        activitySubmissionService.create(dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivitySubmission created successfully"));
    }

    @GetMapping("/activity-submissions")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(200).body(activitySubmissionService.getAll());
    }

    @GetMapping("/activity-submissions/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(activitySubmissionService.getById(id));
    }

    @PutMapping("/activity-submissions/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody ActivitySubmissionInDTO dto) {
        activitySubmissionService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivitySubmission updated successfully"));
    }

    @DeleteMapping("/activity-submissions/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        activitySubmissionService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("ActivitySubmission deleted successfully"));
    }

    // ---------- FLOW: SUBMISSION ----------

    @PostMapping("/activity-assignments/{assignmentId}/start")
    public ResponseEntity<StudentActivityAttemptOutDTO> startAssignment(
            @PathVariable Integer assignmentId,
            @RequestParam Integer studentId) {
        return ResponseEntity.status(200).body(activitySubmissionService.startAssignment(assignmentId, studentId));
    }

    @GetMapping("/activity-submissions/{submissionId}/current")
    public ResponseEntity<ActivitySubmissionOutDTO> getCurrentSubmission(@PathVariable Integer submissionId) {
        return ResponseEntity.status(200).body(activitySubmissionService.getCurrentSubmission(submissionId));
    }

    @PostMapping("/activity-submissions/{submissionId}/submit")
    public ResponseEntity<ActivitySubmissionOutDTO> submitActivity(
            @PathVariable Integer submissionId,
            @RequestParam(defaultValue = "en") String language) {
        // Submit validates all questions are answered, then evaluates automatically -> GRADED with feedback.
        return ResponseEntity.status(200).body(activitySubmissionService.submitActivity(submissionId, language));
    }

    @GetMapping("/activity-submissions/{submissionId}/result")
    public ResponseEntity<ActivitySubmissionOutDTO> getSubmissionResult(@PathVariable Integer submissionId) {
        return ResponseEntity.status(200).body(activitySubmissionService.getSubmissionResult(submissionId));
    }

    @GetMapping("/activity-submissions/{submissionId}/feedback")
    public ResponseEntity<ActivitySubmissionOutDTO> getSubmissionFeedback(@PathVariable Integer submissionId) {
        return ResponseEntity.status(200).body(activitySubmissionService.getSubmissionFeedback(submissionId));
    }

    @GetMapping("/students/{studentId}/activity-results")
    public ResponseEntity<List<ActivitySubmissionOutDTO>> getStudentActivityResults(@PathVariable Integer studentId) {
        return ResponseEntity.status(200).body(activitySubmissionService.getStudentActivityResults(studentId));
    }

    @PatchMapping("/activity-submissions/{submissionId}/return-to-student")
    public ResponseEntity<ApiResponse> returnToStudent(
            @PathVariable Integer submissionId,
            @Valid @RequestBody ActivitySubmissionReturnInDTO request) {
        activitySubmissionService.returnToStudent(submissionId, request.getTeacherId(), request.getTeacherFeedback());
        return ResponseEntity.status(200).body(new ApiResponse("Submission returned to student successfully"));
    }

    @PatchMapping("/activity-submissions/{submissionId}/teacher-feedback")
    public ResponseEntity<ActivitySubmissionOutDTO> addTeacherFeedback(
            @PathVariable Integer submissionId,
            @Valid @RequestBody TeacherFeedbackInDTO request) {
        return ResponseEntity.status(200).body(
                activitySubmissionService.addTeacherFeedback(submissionId, request.getTeacherId(), request.getTeacherFeedback()));
    }

    @PatchMapping("/activity-submissions/{submissionId}/reopen")
    public ResponseEntity<StudentActivityAttemptOutDTO> reopenSubmission(@PathVariable Integer submissionId) {
        return ResponseEntity.status(200).body(activitySubmissionService.reopenSubmission(submissionId));
    }

    @GetMapping("/teachers/{teacherId}/activity-submissions/pending-grading")
    public ResponseEntity<List<ActivitySubmissionOutDTO>> getPendingGradingSubmissions(@PathVariable Integer teacherId) {
        return ResponseEntity.status(200).body(activitySubmissionService.getPendingGradingSubmissions(teacherId));
    }
}
