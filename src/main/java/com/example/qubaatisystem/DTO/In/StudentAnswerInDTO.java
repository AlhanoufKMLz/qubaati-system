package com.example.qubaatisystem.DTO.In;

import com.example.qubaatisystem.Enum.AnswerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnswerInDTO {

    @NotBlank(message = "answerText is required")
    @Size(max = 2000, message = "answerText must be at most 2000 characters")
    private String answerText;

    private Boolean isCorrect;

    @PositiveOrZero(message = "earnedPoints must be zero or positive")
    private Integer earnedPoints;

    @NotNull(message = "status is required")
    private AnswerStatus status;

    @NotNull(message = "questionId is required")
    private Integer questionId;

    @NotNull(message = "studentId is required")
    private Integer studentId;

    @NotNull(message = "activitySubmissionId is required")
    private Integer activitySubmissionId;
}
