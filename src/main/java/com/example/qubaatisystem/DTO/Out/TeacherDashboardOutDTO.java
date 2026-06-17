package com.example.qubaatisystem.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDashboardOutDTO {

    private Integer teacherId;
    private String fullName;
    private String specialization;
    private String email;

    private Integer classroomCount;
    private Integer totalStudentCount;
}
