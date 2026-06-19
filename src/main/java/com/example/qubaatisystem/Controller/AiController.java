package com.example.qubaatisystem.Controller;

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

    private final AiActivityService AiActivityService;

    @PostMapping("/activities/generate")
    public ResponseEntity<ActivityDetailsOutDTO> generateActivity(
            @Valid @RequestBody AiGenerateActivityInDTO dto,
            @RequestParam(defaultValue = "en") String language) {
        return ResponseEntity.status(200).body(AiActivityService.generateActivity(dto, language));
    }

    @PostMapping("/activities/{activityId}/refine")
    public ResponseEntity<ActivityDetailsOutDTO> refineActivity(
            @PathVariable Integer activityId,
            @Valid @RequestBody(required = false) AiRefineActivityInDTO request,
            @RequestParam(defaultValue = "en") String language) {
        // Instruction is optional: empty body ({}) or no body at all is valid; AiService applies a default.
        String instruction = request == null ? null : request.getInstruction();
        return ResponseEntity.status(200).body(AiActivityService.refineActivity(activityId, instruction, language));
    }

    @PostMapping("/activity-submissions/{submissionId}/evaluate")
    public ResponseEntity<ActivitySubmissionOutDTO> evaluateSubmission(
            @PathVariable Integer submissionId,
            @RequestParam(defaultValue = "en") String language) {
        return ResponseEntity.status(200).body(AiActivityService.evaluateSubmission(submissionId, language));
    }

    @PostMapping("/activity-submissions/{submissionId}/generate-feedback")
    public ResponseEntity<ActivitySubmissionOutDTO> generateFeedback(
            @PathVariable Integer submissionId,
            @RequestParam(defaultValue = "student") String audience,
            @RequestParam(defaultValue = "en") String language) {
        return ResponseEntity.status(200).body(AiActivityService.generateFeedback(submissionId, audience, language));
    }
}
