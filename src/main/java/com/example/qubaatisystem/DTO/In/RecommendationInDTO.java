package com.example.qubaatisystem.DTO.In;

import com.example.qubaatisystem.Enum.RecommendationPriority;
import com.example.qubaatisystem.Enum.RecommendationStatus;
import com.example.qubaatisystem.Enum.RecommendationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationInDTO {

    @NotBlank(message = "title is required")
    @Size(max = 150, message = "title must not exceed 150 characters")
    private String title;

    @Size(max = 2000, message = "description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "type is required")
    private RecommendationType type;

    @NotNull(message = "priority is required")
    private RecommendationPriority priority;

    @NotNull(message = "status is required")
    private RecommendationStatus status;

    @Size(max = 2000, message = "reason must not exceed 2000 characters")
    private String reason;

    private java.time.LocalDateTime generatedAt;

    @NotNull(message = "studentId is required")
    private Integer studentId;

    private Integer skillId;

    private Integer missionId;

    private Integer activityId;
}
