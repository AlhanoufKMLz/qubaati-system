package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.LearningStyleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningStyleHistoryOutDTO {

    private Integer id;
    private LearningStyleType previousPrimaryStyle;
    private LearningStyleType newPrimaryStyle;
    private LearningStyleType previousSecondaryStyle;
    private LearningStyleType newSecondaryStyle;
    private Double previousConfidence;
    private Double newConfidence;
    private String reason;
    private java.time.LocalDateTime changedAt;
    private Integer studentId;
    private String studentName;
    private Integer learningStyleId;
}
