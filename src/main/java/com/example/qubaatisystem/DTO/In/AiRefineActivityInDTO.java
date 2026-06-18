package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional body for the AI refine endpoint. The instruction is optional: an empty body ({@code {}}) or a
 * missing body is valid, in which case {@code AiService} applies a safe default instruction internally.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRefineActivityInDTO {

    @Size(max = 2000, message = "instruction must be at most 2000 characters")
    private String instruction;
}
