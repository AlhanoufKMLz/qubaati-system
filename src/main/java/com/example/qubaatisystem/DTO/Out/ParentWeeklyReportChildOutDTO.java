package com.example.qubaatisystem.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One child row inside the parent weekly report payload sent to n8n. Parent-safe only: no correct answers,
 * isCorrect, or scoreImpact — purely aggregated learning data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentWeeklyReportChildOutDTO {

    private Integer studentId;
    private String studentName;
    private String gradeLevel;
    private Integer totalPoints;
    private Integer completedMissionsCount;
    private Integer averageActivityScore;
    private Integer completedMissionSessionsCount;
    private List<String> strongSkills;
    private List<String> weakSkills;
    private String learningStyle;
    private String latestInsight;
    private List<String> recommendations;
}
