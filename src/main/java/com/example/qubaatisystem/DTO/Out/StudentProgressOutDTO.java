package com.example.qubaatisystem.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentProgressOutDTO {

    private Integer studentId;
    private String fullName;
    private Integer totalPoints;
    private Integer completedMissionsCount;
}
