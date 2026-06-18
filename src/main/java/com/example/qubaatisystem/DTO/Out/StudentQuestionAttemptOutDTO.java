package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.DifficultyLevel;
import com.example.qubaatisystem.Enum.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Student-safe view of a question while attempting an activity. Deliberately omits {@code correctAnswer} so
 * the student cannot see the answer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuestionAttemptOutDTO {

    private Integer questionId;
    private String content;
    private QuestionType type;
    private DifficultyLevel difficulty;
    private Integer points;
    private List<StudentOptionAttemptOutDTO> options;
}
