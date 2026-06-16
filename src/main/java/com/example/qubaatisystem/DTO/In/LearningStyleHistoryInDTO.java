package com.example.qubaatisystem.DTO.In;

import com.example.qubaatisystem.Enum.LearningStyleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningStyleHistoryInDTO {

    private LearningStyleType previousPrimaryStyle;

    @NotNull(message = "newPrimaryStyle is required")
    private LearningStyleType newPrimaryStyle;

    private LearningStyleType previousSecondaryStyle;

    private LearningStyleType newSecondaryStyle;

    @PositiveOrZero(message = "previousConfidence must be zero or positive")
    private Double previousConfidence;

    @PositiveOrZero(message = "newConfidence must be zero or positive")
    private Double newConfidence;

    @Size(max = 2000, message = "reason must be at most 2000 characters")
    private String reason;

    private java.time.LocalDateTime changedAt;

    @NotNull(message = "studentId is required")
    private Integer studentId;

    private Integer learningStyleId;
}
