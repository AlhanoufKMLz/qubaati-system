package com.example.qubaatisystem.DTO.In;

import com.example.qubaatisystem.Enum.ActivityReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityReviewInDTO {

    @NotNull(message = "decision is required")
    private ActivityReviewDecision decision;

    @Size(max = 1000, message = "reviewComment must be at most 1000 characters")
    private String reviewComment;

    private java.time.LocalDateTime reviewedAt;

    @NotNull(message = "activityId is required")
    private Integer activityId;

    @NotNull(message = "teacherId is required")
    private Integer teacherId;
}
