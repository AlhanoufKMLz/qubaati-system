package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillProgressHistoryInDTO {

    @PositiveOrZero(message = "previousScore must be zero or positive")
    private Double previousScore;

    @PositiveOrZero(message = "newScore must be zero or positive")
    private Double newScore;

    @PositiveOrZero(message = "previousLevel must be zero or positive")
    private Integer previousLevel;

    @PositiveOrZero(message = "newLevel must be zero or positive")
    private Integer newLevel;

    @Size(max = 2000, message = "reason must not exceed 2000 characters")
    private String reason;

    private java.time.LocalDateTime changedAt;

    @NotNull(message = "studentId is required")
    private Integer studentId;

    @NotNull(message = "skillId is required")
    private Integer skillId;

    private Integer studentSkillId;
}
