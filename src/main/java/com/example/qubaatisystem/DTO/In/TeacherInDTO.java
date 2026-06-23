package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInDTO {

    // ----- User account fields (used to create the linked User internally) -----
    @NotBlank(message = "username is required")
    @Size(max = 50, message = "username must be at most 50 characters")
    private String username;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100, message = "password must be between 6 and 100 characters")
    private String password;

    // ----- Teacher profile fields -----
    @NotBlank(message = "fullName is required")
    @Size(max = 120, message = "fullName must be at most 120 characters")
    private String fullName;

    @NotBlank(message = "specialization is required")
    @Size(max = 120, message = "specialization must be at most 120 characters")
    private String specialization;

    @NotBlank(message = "phoneNumber is required")
    @Size(max = 30, message = "phoneNumber must be at most 30 characters")
    private String phoneNumber;
}
