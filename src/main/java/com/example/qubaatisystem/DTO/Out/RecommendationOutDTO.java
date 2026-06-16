package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.RecommendationPriority;
import com.example.qubaatisystem.Enum.RecommendationStatus;
import com.example.qubaatisystem.Enum.RecommendationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationOutDTO {

    private Integer id;
    private String title;
    private String description;
    private RecommendationType type;
    private RecommendationPriority priority;
    private RecommendationStatus status;
    private String reason;
    private java.time.LocalDateTime generatedAt;
    private Integer studentId;
    private String studentName;
    private Integer skillId;
    private String skillName;
    private Integer missionId;
    private String missionTitle;
    private Integer activityId;
    private String activityTitle;
}
