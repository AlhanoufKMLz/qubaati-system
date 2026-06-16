package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.ActivityAssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAssignmentOutDTO {

    private Integer id;
    private java.time.LocalDateTime assignedAt;
    private java.time.LocalDateTime dueDate;
    private ActivityAssignmentStatus status;
    private Integer activityId;
    private String activityTitle;
    private Integer studentId;
    private String studentName;
    private Integer classroomId;
    private String classroomName;
    private Integer assignedByTeacherId;
    private String assignedByTeacherName;
}
