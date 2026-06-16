package com.example.qubaatisystem.DTO.In;

import com.example.qubaatisystem.Enum.ActivitySubmissionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySubmissionInDTO {

    private java.time.LocalDateTime startedAt;

    private java.time.LocalDateTime submittedAt;

    @PositiveOrZero(message = "score must be zero or positive")
    private Integer score;

    @NotNull(message = "status is required")
    private ActivitySubmissionStatus status;

    @Size(max = 2000, message = "aiFeedback must be at most 2000 characters")
    private String aiFeedback;

    @Size(max = 2000, message = "teacherFeedback must be at most 2000 characters")
    private String teacherFeedback;

    @NotNull(message = "activityAssignmentId is required")
    private Integer activityAssignmentId;

    @NotNull(message = "studentId is required")
    private Integer studentId;
}
