package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.ActivityReviewDecision;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityReviewOutDTO {

    private Integer id;
    private ActivityReviewDecision decision;
    private String reviewComment;
    private java.time.LocalDateTime reviewedAt;
    private Integer activityId;
    private String activityTitle;
    private Integer teacherId;
    private String teacherName;
}
