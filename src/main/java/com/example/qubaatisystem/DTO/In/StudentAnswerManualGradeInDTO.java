package com.example.qubaatisystem.DTO.In;

import com.example.qubaatisystem.Enum.AnswerStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for the teacher manual-grade / score-override endpoint on a single answer.
 * status must be one of CORRECT / INCORRECT / PARTIAL (validated in the service); earnedPoints must not
 * exceed the question's points. feedback is optional.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnswerManualGradeInDTO {

    // DEPRECATED actor id — IGNORED for the authenticated TEACHER (derived from Basic Auth);
    // only an ADMIN may supply it to act on a teacher behalf. Optional; do not send it as a teacher.
    private Integer teacherId;

    @NotNull(message = "earnedPoints is required")
    @PositiveOrZero(message = "earnedPoints must be zero or positive")
    private Integer earnedPoints;

    @NotNull(message = "status is required (CORRECT, INCORRECT or PARTIAL)")
    private AnswerStatus status;

    @Size(max = 2000, message = "feedback must be at most 2000 characters")
    private String feedback;
}
