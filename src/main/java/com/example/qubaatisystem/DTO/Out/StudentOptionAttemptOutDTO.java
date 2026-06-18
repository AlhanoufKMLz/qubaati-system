package com.example.qubaatisystem.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Student-safe view of an option while attempting an activity. Deliberately omits {@code isCorrect} so the
 * student cannot see which option is correct.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentOptionAttemptOutDTO {

    private Integer optionId;
    private String content;
}
