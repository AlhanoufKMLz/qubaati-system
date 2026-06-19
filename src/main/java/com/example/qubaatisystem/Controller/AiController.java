package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Service.AiAnalysisService;
import com.example.qubaatisystem.DTO.In.AiGenerateActivityInDTO;
import com.example.qubaatisystem.DTO.In.AiRefineActivityInDTO;
import com.example.qubaatisystem.DTO.Out.ActivityDetailsOutDTO;
import com.example.qubaatisystem.DTO.Out.ActivitySubmissionOutDTO;
import com.example.qubaatisystem.Service.AiActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/activities/generate")
    public ResponseEntity<ActivityDetailsOutDTO> generateActivity(
            @Valid @RequestBody AiGenerateActivityInDTO dto,
            @RequestParam(defaultValue = "en") String language) {
        return ResponseEntity.status(200).body(aiActivityService.generateActivity(dto, language));
    }

    @PostMapping("/activities/{activityId}/refine")
    public ResponseEntity<ActivityDetailsOutDTO> refineActivity(
            @PathVariable Integer activityId,
            @Valid @RequestBody(required = false) AiRefineActivityInDTO request,
            @RequestParam(defaultValue = "en") String language) {
        // Instruction is optional: empty body ({}) or no body at all is valid; AiActivityService applies a default.
        String instruction = request == null ? null : request.getInstruction();
        return ResponseEntity.status(200).body(aiActivityService.refineActivity(activityId, instruction, language));
    }

    @PostMapping("/activity-submissions/{submissionId}/evaluate")
    public ResponseEntity<ActivitySubmissionOutDTO> evaluateSubmission(
            @PathVariable Integer submissionId,
            @RequestParam(defaultValue = "en") String language) {
        return ResponseEntity.status(200).body(aiActivityService.evaluateSubmission(submissionId, language));
    }

    @PostMapping("/activity-submissions/{submissionId}/generate-feedback")
    public ResponseEntity<ActivitySubmissionOutDTO> generateFeedback(@PathVariable Integer submissionId, @RequestParam(defaultValue = "student") String audience, @RequestParam(defaultValue = "en") String language) {
        return ResponseEntity.status(200).body(aiActivityService.generateFeedback(submissionId, audience, language));
    }

    @PostMapping("/classrooms/{classroomId}/summary")
    public ResponseEntity<?> getClassroomSummary(@PathVariable Integer classroomId) {
        return ResponseEntity.status(200).body(aiAnalysisService.analyzeClassroom(classroomId));
    }

    @PostMapping("/parents/{parentId}/children/{studentId}/summary")
    public ResponseEntity<?> getStudentSummary(@PathVariable Integer parentId,
                                               @PathVariable Integer studentId) {
        return ResponseEntity.status(200).body(aiAnalysisService.analyzeStudent(parentId, studentId));
    }

    @PostMapping("/parents/{parentId}/dashboard-insight")
    public ResponseEntity<?> getFamilyDashboardInsight(@PathVariable Integer parentId) {
        return ResponseEntity.status(200).body(aiAnalysisService.analyzeFamilyInsight(parentId));
    }
}
