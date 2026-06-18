package com.example.qubaatisystem.DTO.In;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchStudentAnswerInDTO {

    @NotEmpty(message = "answers must not be null or empty")
    @Valid
    private List<SingleStudentAnswerInDTO> answers;
}
