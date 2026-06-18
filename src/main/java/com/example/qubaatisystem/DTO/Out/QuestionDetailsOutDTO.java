package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.DifficultyLevel;
import com.example.qubaatisystem.Enum.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDetailsOutDTO {

    private Integer id;
    private String content;
    private QuestionType type;
    private DifficultyLevel difficulty;
    private Integer points;
    private String correctAnswer;
    private List<OptionDetailsOutDTO> options;
}
