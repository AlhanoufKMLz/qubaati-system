package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.ActivityStatus;
import com.example.qubaatisystem.Enum.ActivityType;
import com.example.qubaatisystem.Enum.DifficultyLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityOutDTO {

    private Integer id;
    private String title;
    private String description;
    private ActivityType type;
    private ActivityStatus status;
    private DifficultyLevel difficulty;
    private Integer maxScore;
    private java.time.LocalDateTime createdAt;
}
