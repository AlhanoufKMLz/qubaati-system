package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.ActivitySubmissionStatus;
import com.example.qubaatisystem.Enum.ActivityType;
import com.example.qubaatisystem.Enum.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Returned by start-assignment: the student-safe activity content to attempt (questions + options) plus the
 * created submission context. Never exposes correctAnswer or which option isCorrect.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentActivityAttemptOutDTO {

    private Integer submissionId;
    private Integer assignmentId;
    private Integer activityId;
    private String activityTitle;
    private String activityDescription;
    private ActivityType activityType;
    private DifficultyLevel difficulty;
    private Integer activityMaxScore;
    private LocalDateTime startedAt;
    private ActivitySubmissionStatus submissionStatus;
    private Integer studentId;
    private String studentName;
    private List<StudentQuestionAttemptOutDTO> questions;
}
