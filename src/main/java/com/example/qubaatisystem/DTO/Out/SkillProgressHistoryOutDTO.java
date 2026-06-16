package com.example.qubaatisystem.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillProgressHistoryOutDTO {

    private Integer id;
    private Double previousScore;
    private Double newScore;
    private Integer previousLevel;
    private Integer newLevel;
    private String reason;
    private java.time.LocalDateTime changedAt;
    private Integer studentId;
    private String studentName;
    private Integer skillId;
    private String skillName;
    private Integer studentSkillId;
}
