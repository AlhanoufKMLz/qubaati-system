package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherOutDTO {

    private Integer id;

    // ----- Safe linked User fields (never the password) -----
    private Integer userId;
    private String username;
    private String email;
    private UserRole role;

    // ----- Teacher profile fields -----
    private String fullName;
    private String specialization;
    private String phoneNumber;
}
