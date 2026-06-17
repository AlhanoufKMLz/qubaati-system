package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.AnswerStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnswerOutDTO {

    private Integer id;
    private String answerText;
    private Integer earnedPoints;
    private AnswerStatus status;
    private LocalDateTime answeredAt;

    private Integer questionId;

    private Integer studentId;
    private String studentName;

    private Integer activitySubmissionId;
}
