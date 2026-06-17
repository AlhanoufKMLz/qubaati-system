package com.example.qubaatisystem.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomDashboardOutDTO {

    private Integer classroomId;
    private String name;
    private Integer gradeLevel;
    private String section;
    private Integer teacherId;
    private String teacherName;

    private Integer studentCount;
}
