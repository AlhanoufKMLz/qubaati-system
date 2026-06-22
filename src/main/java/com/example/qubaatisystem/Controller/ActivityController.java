package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.ActivityInDTO;
import com.example.qubaatisystem.DTO.In.ActivityReviewActionInDTO;
import com.example.qubaatisystem.DTO.Out.ActivityDetailsOutDTO;
import com.example.qubaatisystem.Enum.ActivityStatus;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.ActivityService;
import com.example.qubaatisystem.Service.AiActivityService;
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

@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final AiActivityService aiActivityService;
    private final SecurityOwnershipService security;

    // ---------- CRUD ----------

    // The owner is derived from Basic Auth: a TEACHER owns the activity (body teacherId ignored); an ADMIN may
    // supply teacherId to create on a teacher's behalf. PARENT/STUDENT are blocked by role + this resolver.
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ActivityInDTO dto) {
        dto.setTeacherId(security.resolveOwningTeacherId(dto.getTeacherId()));
        activityService.create(dto);
        return ResponseEntity.status(200).body(new ApiResponse("Activity created successfully"));
    }

    // Optional status filter (enum, not free text): GET /activities?status=PENDING_REVIEW serves the review
    // queue / status views. No param returns all activities (backward compatible).
    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) ActivityStatus status) {
        return ResponseEntity.status(200).body(activityService.getByStatus(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(activityService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody ActivityInDTO dto) {
        activityService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Activity updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        activityService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("Activity deleted successfully"));
    }

    // ---------- REVIEW / APPROVAL FLOW ----------
    // There is no public submit-for-review endpoint: AI generation auto-submits the activity to the
    // review queue (Issue 3). The transition stays available as ActivityService.submitForReview(...).

    @GetMapping("/review-queue")
    public ResponseEntity<?> getReviewQueue() {
        return ResponseEntity.status(200).body(activityService.getReviewQueue());
    }

    @PatchMapping("/{activityId}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Integer activityId,
            @Valid @RequestBody ActivityReviewActionInDTO request) {
        // Reviewer = the authenticated teacher who owns the activity (body teacherId ignored; admin has no
        // teacher reviewer so review is teacher-owner-only).
        Integer reviewerId = security.getCurrentTeacherId();
        security.assertCurrentTeacherOwnsActivityOrAdmin(activityId);
        activityService.approveActivity(activityId, reviewerId, request.getReviewComment());
        return ResponseEntity.status(200).body(new ApiResponse("Activity approved successfully"));
    }

    @PatchMapping("/{activityId}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Integer activityId,
            @Valid @RequestBody ActivityReviewActionInDTO request) {
        Integer reviewerId = security.getCurrentTeacherId();
        security.assertCurrentTeacherOwnsActivityOrAdmin(activityId);
        activityService.rejectActivity(activityId, reviewerId, request.getReviewComment());
        return ResponseEntity.status(200).body(new ApiResponse("Activity rejected successfully"));
    }

    @PatchMapping("/{activityId}/request-revision")
    public ResponseEntity<?> requestRevision(
            @PathVariable Integer activityId,
            @Valid @RequestBody ActivityReviewActionInDTO request) {
        Integer reviewerId = security.getCurrentTeacherId();
        security.assertCurrentTeacherOwnsActivityOrAdmin(activityId);
        activityService.requestRevision(activityId, reviewerId, request.getReviewComment());
        return ResponseEntity.status(200).body(new ApiResponse("Revision requested successfully"));
    }

    @GetMapping("/{activityId}/review-history")
    public ResponseEntity<?> getReviewHistory(@PathVariable Integer activityId) {
        return ResponseEntity.status(200).body(activityService.getReviewHistory(activityId));
    }

    // Teacher/reviewer full activity details — includes questions, options, correctAnswer and isCorrect.
    // NOT student-safe (the student start/reopen views deliberately hide correct answers).
    @GetMapping("/{activityId}/details")
    public ResponseEntity<ActivityDetailsOutDTO> getActivityDetails(
            @PathVariable Integer activityId,
            @RequestParam(defaultValue = "en") String language) {
        return ResponseEntity.status(200).body(aiActivityService.getActivityDetails(activityId, language));
    }
}
