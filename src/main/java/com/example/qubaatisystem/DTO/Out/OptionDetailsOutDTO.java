package com.example.qubaatisystem.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OptionDetailsOutDTO {

    private Integer id;
    private String content;
    private Boolean isCorrect;
}
