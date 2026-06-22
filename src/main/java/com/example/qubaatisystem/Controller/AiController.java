package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Service.AiAnalysisService;
import com.example.qubaatisystem.DTO.In.AiGenerateActivityInDTO;
import com.example.qubaatisystem.DTO.In.AiRefineActivityInDTO;
import com.example.qubaatisystem.DTO.Out.ActivityDetailsOutDTO;
import com.example.qubaatisystem.DTO.Out.ActivitySubmissionOutDTO;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.AiActivityService;
import com.example.qubaatisystem.Service.AiProviderHealthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiActivityService aiActivityService;
    private final AiAnalysisService aiAnalysisService;
    private final AiProviderHealthService aiProviderHealthService;
    private final SecurityOwnershipService security;

    // AI provider health/config — reports whether a key is configured and which model is set (key never
    // exposed). Pass ?probe=true to additionally send a tiny live ChatClient request.
    @GetMapping("/health")
    public ResponseEntity<?> health(@RequestParam(defaultValue = "false") boolean probe) {
        return ResponseEntity.status(200).body(aiProviderHealthService.health(probe));
    }

    // The generated activity's owner is derived from Basic Auth (teacher owns it; admin may pass a teacherId).
    @PostMapping("/activities/generate")
    public ResponseEntity<ActivityDetailsOutDTO> generateActivity(
            @Valid @RequestBody AiGenerateActivityInDTO dto,
            @RequestParam(defaultValue = "en") String language) {
        dto.setTeacherId(security.resolveOwningTeacherId(dto.getTeacherId()));
        return ResponseEntity.status(200).body(aiActivityService.generateActivity(dto, language));
    }

    @PostMapping("/activities/{activityId}/refine")
    public ResponseEntity<ActivityDetailsOutDTO> refineActivity(
            @PathVariable Integer activityId,
            @Valid @RequestBody(required = false) AiRefineActivityInDTO request,
            @RequestParam(defaultValue = "en") String language) {
        security.assertCurrentTeacherOwnsActivityOrAdmin(activityId);
        // Instruction is optional: empty body ({}) or no body at all is valid; AiActivityService applies a default.
        String instruction = request == null ? null : request.getInstruction();
        return ResponseEntity.status(200).body(aiActivityService.refineActivity(activityId, instruction, language));
    }

    @PostMapping("/activity-submissions/{submissionId}/evaluate")
    public ResponseEntity<ActivitySubmissionOutDTO> evaluateSubmission(
            @PathVariable Integer submissionId,
            @RequestParam(defaultValue = "en") String language) {
        security.assertCurrentTeacherOwnsSubmissionOrAdmin(submissionId);
        return ResponseEntity.status(200).body(aiActivityService.evaluateSubmission(submissionId, language));
    }

    @PostMapping("/activity-submissions/{submissionId}/generate-feedback")
    public ResponseEntity<ActivitySubmissionOutDTO> generateFeedback(@PathVariable Integer submissionId, @RequestParam(defaultValue = "student") String audience, @RequestParam(defaultValue = "en") String language) {
        security.assertCurrentTeacherOwnsSubmissionOrAdmin(submissionId);
        return ResponseEntity.status(200).body(aiActivityService.generateFeedback(submissionId, audience, language));
    }

    // classroomId is an ENTITY id (not a profile id) — a teacher may summarize only their own classroom.
    @PostMapping("/classrooms/{classroomId}/summary")
    public ResponseEntity<?> getClassroomSummary(@PathVariable Integer classroomId) {
        security.assertCurrentTeacherOwnsClassroomOrAdmin(classroomId);
        return ResponseEntity.status(200).body(aiAnalysisService.analyzeClassroom(classroomId));
    }

    // ===== Legacy profile-ID AI endpoints — now ownership-protected (deprecated; prefer the /me versions) =====

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @PostMapping("/parents/{parentId}/children/{studentId}/summary")
    public ResponseEntity<?> getStudentSummary(@PathVariable Integer parentId,
                                               @PathVariable Integer studentId) {
        security.assertParentOwnsStudentOrAdmin(parentId, studentId);
        return ResponseEntity.status(200).body(aiAnalysisService.analyzeStudent(parentId, studentId));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @PostMapping("/parents/{parentId}/dashboard-insight")
    public ResponseEntity<?> getFamilyDashboardInsight(@PathVariable Integer parentId) {
        security.assertCurrentParentOrAdmin(parentId);
        return ResponseEntity.status(200).body(aiAnalysisService.analyzeFamilyInsight(parentId));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @PostMapping("/teachers/{teacherId}/dashboard-insight")
    public ResponseEntity<?> getTeacherDashboardInsight(@PathVariable Integer teacherId) {
        security.assertCurrentTeacherOrAdmin(teacherId);
        return ResponseEntity.status(200).body(aiAnalysisService.analyzeTeacherDashboard(teacherId));
    }

    // ===== Current-user ("me") AI endpoints — no profile id in the path =====

    @PostMapping("/teachers/me/dashboard-insight")
    public ResponseEntity<?> getMyTeacherDashboardInsight() {
        return ResponseEntity.status(200).body(aiAnalysisService.analyzeTeacherDashboard(security.getCurrentTeacherId()));
    }

    @PostMapping("/parents/me/dashboard-insight")
    public ResponseEntity<?> getMyFamilyDashboardInsight() {
        return ResponseEntity.status(200).body(aiAnalysisService.analyzeFamilyInsight(security.getCurrentParentId()));
    }

    @PostMapping("/parents/me/children/{studentId}/summary")
    public ResponseEntity<?> getMyChildSummary(@PathVariable Integer studentId) {
        security.assertCurrentParentOwnsChildOrAdmin(studentId);
        return ResponseEntity.status(200).body(aiAnalysisService.analyzeStudent(security.getCurrentParentId(), studentId));
    }
}
