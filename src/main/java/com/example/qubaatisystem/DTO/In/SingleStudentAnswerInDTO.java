package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SingleStudentAnswerInDTO {

    @NotNull(message = "questionId is required")
    private Integer questionId;

    // At least one of answerText / selectedOptionId must be provided (validated in StudentAnswerService).
    private String answerText;

    // If provided, the option must belong to the question (validated in StudentAnswerService).
    private Integer selectedOptionId;
}
