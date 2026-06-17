package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Only safe child profile fields. Account fields (username/email/password) and
// game-managed fields (totalPoints/completedMissionsCount) are intentionally excluded.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildUpdateProfileInDTO {

    @NotBlank(message = "fullName is required")
    @Size(max = 120, message = "fullName must be at most 120 characters")
    private String fullName;

    @NotNull(message = "age is required")
    @Min(value = 3, message = "age must be at least 3")
    @Max(value = 100, message = "age must be at most 100")
    private Integer age;

    @NotBlank(message = "grade is required")
    @Size(max = 50, message = "grade must be at most 50 characters")
    private String grade;
}
