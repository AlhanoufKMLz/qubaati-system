package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.DecisionSubmitInDTO;
import com.example.qubaatisystem.DTO.In.MissionRegenerateInDTO;
import com.example.qubaatisystem.DTO.In.MissionStepBatchInDTO;
import com.example.qubaatisystem.DTO.Out.AvailableMissionOutDTO;
import com.example.qubaatisystem.DTO.Out.AvailableMissionsOutDTO;
import com.example.qubaatisystem.DTO.Out.DecisionSubmitOutDTO;
import com.example.qubaatisystem.DTO.Out.InsightOutDTO;
import com.example.qubaatisystem.DTO.Out.MissionCompletionOutDTO;
import com.example.qubaatisystem.DTO.Out.MissionStepsAdminOutDTO;
import com.example.qubaatisystem.DTO.Out.NotificationOutDTO;
import com.example.qubaatisystem.DTO.Out.RecommendationOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentMissionAttemptOutDTO;
import com.example.qubaatisystem.DTO.Out.StudentSkillOutDTO;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.MissionService;
import com.example.qubaatisystem.Service.MissionSessionService;
import com.example.qubaatisystem.Service.NotificationService;
import com.example.qubaatisystem.Service.RecommendationService;
import com.example.qubaatisystem.Service.StudentSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Student-3 mission flow (REST controller, not a service): available missions, regeneration, the mission
 * session lifecycle (start/current/decision/complete/abandon/insight), and the student-facing
 * recommendation/notification/skill endpoints. Uses absolute paths because the routes span several resources.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MissionFlowController {

    private final MissionService missionService;
    private final MissionSessionService missionSessionService;
    private final RecommendationService recommendationService;
    private final NotificationService notificationService;
    private final StudentSkillService studentSkillService;
    private final SecurityOwnershipService security;

    // ---------- available / default missions ----------

    @Deprecated // prefer GET /students/me/missions/available
    @GetMapping("/students/{studentId}/missions/available")
    public ResponseEntity<AvailableMissionsOutDTO> getAvailableMissions(
            @PathVariable Integer studentId,
            @RequestParam Integer careerWorldId) {
        security.assertCurrentStudentOrAdmin(studentId);
        return ResponseEntity.status(200).body(missionService.getAvailableMissions(studentId, careerWorldId));
    }

    // Current-student alias — no studentId in the path.
    @GetMapping("/students/me/missions/available")
    public ResponseEntity<AvailableMissionsOutDTO> getMyAvailableMissions(@RequestParam Integer careerWorldId) {
        return ResponseEntity.status(200)
                .body(missionService.getAvailableMissions(security.getCurrentStudentId(), careerWorldId));
    }

    @GetMapping("/career-worlds/{careerWorldId}/missions")
    public ResponseEntity<List<AvailableMissionOutDTO>> getCareerWorldDefaultMissions(@PathVariable Integer careerWorldId) {
        return ResponseEntity.status(200).body(missionService.getCareerWorldDefaultMissions(careerWorldId));
    }

    // Current-student regenerate — no studentId in the path.
    @PatchMapping("/students/me/missions/{missionId}/regenerate")
    public ResponseEntity<AvailableMissionOutDTO> regenerateMyMission(
            @PathVariable Integer missionId,
            @Valid @RequestBody(required = false) MissionRegenerateInDTO request) {
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.status(200).body(missionService.regenerateMission(security.getCurrentStudentId(), missionId, reason));
    }

    @Deprecated // prefer PATCH /students/me/missions/{missionId}/regenerate
    @PatchMapping("/students/{studentId}/missions/{missionId}/regenerate")
    public ResponseEntity<AvailableMissionOutDTO> regenerateMission(
            @PathVariable Integer studentId,
            @PathVariable Integer missionId,
            @Valid @RequestBody(required = false) MissionRegenerateInDTO request) {
        security.assertCurrentStudentOrAdmin(studentId);
        String reason = request == null ? null : request.getReason();
        return ResponseEntity.status(200).body(missionService.regenerateMission(studentId, missionId, reason));
    }

    // ---------- step authoring / seeding (teacher/admin) ----------

    @PostMapping("/missions/{missionId}/steps/batch")
    public ResponseEntity<MissionStepsAdminOutDTO> replaceMissionSteps(
            @PathVariable Integer missionId,
            @Valid @RequestBody MissionStepBatchInDTO request) {
        return ResponseEntity.status(200).body(missionService.replaceSteps(missionId, request));
    }

    @GetMapping("/missions/{missionId}/steps")
    public ResponseEntity<MissionStepsAdminOutDTO> getMissionSteps(@PathVariable Integer missionId) {
        return ResponseEntity.status(200).body(missionService.getSteps(missionId));
    }

    @DeleteMapping("/missions/{missionId}/steps")
    public ResponseEntity<ApiResponse> deleteMissionSteps(@PathVariable Integer missionId) {
        missionService.deleteSteps(missionId);
        return ResponseEntity.status(200).body(new ApiResponse("Mission steps deleted"));
    }

    // ---------- mission session lifecycle ----------

    // studentId is OPTIONAL: a student starts as themselves (derived from Basic Auth); an admin may pass studentId.
    @PostMapping("/missions/{missionId}/sessions/start")
    public ResponseEntity<StudentMissionAttemptOutDTO> startSession(
            @PathVariable Integer missionId,
            @RequestParam(required = false) Integer studentId) {
        Integer effectiveStudentId = (studentId != null) ? studentId : security.getCurrentStudentId();
        security.assertCurrentStudentOrAdmin(effectiveStudentId);
        return ResponseEntity.status(200).body(missionSessionService.startSession(missionId, effectiveStudentId));
    }

    @GetMapping("/mission-sessions/{sessionId}/current")
    public ResponseEntity<StudentMissionAttemptOutDTO> getCurrentSession(@PathVariable Integer sessionId) {
        return ResponseEntity.status(200).body(missionSessionService.getCurrentSession(sessionId));
    }

    @PostMapping("/mission-sessions/{sessionId}/decisions")
    public ResponseEntity<DecisionSubmitOutDTO> submitDecision(
            @PathVariable Integer sessionId,
            @Valid @RequestBody DecisionSubmitInDTO request) {
        return ResponseEntity.status(200)
                .body(missionSessionService.submitDecision(sessionId, request.getChoiceId(), request.getReason()));
    }

    @PatchMapping("/mission-sessions/{sessionId}/complete")
    public ResponseEntity<MissionCompletionOutDTO> completeSession(@PathVariable Integer sessionId) {
        return ResponseEntity.status(200).body(missionSessionService.completeSession(sessionId));
    }

    @PatchMapping("/mission-sessions/{sessionId}/abandon")
    public ResponseEntity<ApiResponse> abandonSession(@PathVariable Integer sessionId) {
        missionSessionService.abandonSession(sessionId);
        return ResponseEntity.status(200).body(new ApiResponse("Mission session abandoned"));
    }

    @GetMapping("/mission-sessions/{sessionId}/insight")
    public ResponseEntity<InsightOutDTO> getInsight(@PathVariable Integer sessionId) {
        return ResponseEntity.status(200).body(missionSessionService.getInsight(sessionId));
    }

    /** OPTIONAL manual re-run of the AI-first insight (the normal flow generates it on completion). */
    @PatchMapping("/mission-sessions/{sessionId}/insight/regenerate")
    public ResponseEntity<InsightOutDTO> regenerateInsight(@PathVariable Integer sessionId) {
        return ResponseEntity.status(200).body(missionSessionService.regenerateInsight(sessionId));
    }

    // ---------- recommendations ----------

    // Current-student recommendation endpoints (no studentId in the path).
    @GetMapping("/students/me/recommendations")
    public ResponseEntity<List<RecommendationOutDTO>> getMyRecommendations() {
        return ResponseEntity.status(200).body(recommendationService.getByStudent(security.getCurrentStudentId()));
    }

    @PostMapping("/students/me/recommendations/regenerate")
    public ResponseEntity<List<RecommendationOutDTO>> regenerateMyRecommendations() {
        return ResponseEntity.status(200).body(missionSessionService.regenerateRecommendations(security.getCurrentStudentId()));
    }

    @Deprecated // prefer GET /students/me/recommendations
    @GetMapping("/students/{studentId}/recommendations")
    public ResponseEntity<List<RecommendationOutDTO>> getStudentRecommendations(@PathVariable Integer studentId) {
        security.assertCurrentStudentOrAdmin(studentId);
        return ResponseEntity.status(200).body(recommendationService.getByStudent(studentId));
    }

    /** OPTIONAL manual re-run of the AI-first recommendations (the normal flow generates them on completion). */
    @Deprecated // prefer POST /students/me/recommendations/regenerate
    @PostMapping("/students/{studentId}/recommendations/regenerate")
    public ResponseEntity<List<RecommendationOutDTO>> regenerateRecommendations(@PathVariable Integer studentId) {
        security.assertCurrentStudentOrAdmin(studentId);
        return ResponseEntity.status(200).body(missionSessionService.regenerateRecommendations(studentId));
    }

    // accept / dismiss / complete are STUDENT-or-ADMIN actions, restricted to the owning student.
    @PatchMapping("/recommendations/{recommendationId}/accept")
    public ResponseEntity<RecommendationOutDTO> acceptRecommendation(@PathVariable Integer recommendationId) {
        security.assertCurrentStudentOwnsRecommendationOrAdmin(recommendationId);
        return ResponseEntity.status(200).body(recommendationService.accept(recommendationId));
    }

    @PatchMapping("/recommendations/{recommendationId}/dismiss")
    public ResponseEntity<RecommendationOutDTO> dismissRecommendation(@PathVariable Integer recommendationId) {
        security.assertCurrentStudentOwnsRecommendationOrAdmin(recommendationId);
        return ResponseEntity.status(200).body(recommendationService.dismiss(recommendationId));
    }

    @PatchMapping("/recommendations/{recommendationId}/complete")
    public ResponseEntity<RecommendationOutDTO> completeRecommendation(@PathVariable Integer recommendationId) {
        security.assertCurrentStudentOwnsRecommendationOrAdmin(recommendationId);
        return ResponseEntity.status(200).body(recommendationService.complete(recommendationId));
    }

    // ---------- notifications (userId ownership: a user only sees their OWN notifications) ----------

    // Current-user ("me") notification endpoints — no userId in the path.
    @GetMapping("/users/me/notifications")
    public ResponseEntity<List<NotificationOutDTO>> getMyNotifications() {
        return ResponseEntity.status(200).body(notificationService.getByUser(security.getCurrentUserId()));
    }

    @GetMapping("/users/me/notifications/unread")
    public ResponseEntity<List<NotificationOutDTO>> getMyUnreadNotifications() {
        return ResponseEntity.status(200).body(notificationService.getUnreadByUser(security.getCurrentUserId()));
    }

    @PatchMapping("/users/me/notifications/{notificationId}/read")
    public ResponseEntity<NotificationOutDTO> markMyNotificationRead(@PathVariable Integer notificationId) {
        security.assertCurrentUserOwnsNotificationOrAdmin(notificationId);
        return ResponseEntity.status(200).body(notificationService.markRead(notificationId));
    }

    @PatchMapping("/users/me/notifications/read-all")
    public ResponseEntity<ApiResponse> markAllMyNotificationsRead() {
        notificationService.markAllRead(security.getCurrentUserId());
        return ResponseEntity.status(200).body(new ApiResponse("All notifications marked as read"));
    }

    // Legacy userId endpoints — now ownership-protected (the userId must be the authenticated user, or admin).
    @GetMapping("/users/{userId}/notifications")
    public ResponseEntity<List<NotificationOutDTO>> getUserNotifications(@PathVariable Integer userId) {
        security.assertCurrentUserOrAdmin(userId);
        return ResponseEntity.status(200).body(notificationService.getByUser(userId));
    }

    @GetMapping("/users/{userId}/notifications/unread")
    public ResponseEntity<List<NotificationOutDTO>> getUserUnreadNotifications(@PathVariable Integer userId) {
        security.assertCurrentUserOrAdmin(userId);
        return ResponseEntity.status(200).body(notificationService.getUnreadByUser(userId));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<NotificationOutDTO> markNotificationRead(@PathVariable Integer notificationId) {
        security.assertCurrentUserOwnsNotificationOrAdmin(notificationId);
        return ResponseEntity.status(200).body(notificationService.markRead(notificationId));
    }

    @PatchMapping("/users/{userId}/notifications/read-all")
    public ResponseEntity<ApiResponse> markAllNotificationsRead(@PathVariable Integer userId) {
        security.assertCurrentUserOrAdmin(userId);
        notificationService.markAllRead(userId);
        return ResponseEntity.status(200).body(new ApiResponse("All notifications marked as read"));
    }

    // ---------- skills ----------

    @GetMapping("/students/me/skills")
    public ResponseEntity<List<StudentSkillOutDTO>> getMySkills() {
        return ResponseEntity.status(200).body(studentSkillService.getByStudentId(security.getCurrentStudentId()));
    }

    @Deprecated // prefer GET /students/me/skills
    @GetMapping("/students/{studentId}/skills")
    public ResponseEntity<List<StudentSkillOutDTO>> getStudentSkills(@PathVariable Integer studentId) {
        security.assertCurrentStudentOrAdmin(studentId);
        return ResponseEntity.status(200).body(studentSkillService.getByStudentId(studentId));
    }
}
