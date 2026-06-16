package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogInDTO {

    @NotBlank(message = "action is required")
    @Size(max = 150, message = "action must be at most 150 characters")
    private String action;

    @Size(max = 100, message = "targetType must be at most 100 characters")
    private String targetType;

    private Integer targetId;

    @Size(max = 2000, message = "details must be at most 2000 characters")
    private String details;

    private java.time.LocalDateTime createdAt;

    private Integer actorId;
}
