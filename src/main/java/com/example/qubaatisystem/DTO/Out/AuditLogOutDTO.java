package com.example.qubaatisystem.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogOutDTO {

    private Integer id;
    private String action;
    private String targetType;
    private Integer targetId;
    private String details;
    private java.time.LocalDateTime createdAt;
    private Integer actorId;
    private String actorUsername;
    private String actorEmail;
}
