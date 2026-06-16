package com.example.qubaatisystem.DTO.In;

import com.example.qubaatisystem.Enum.NotificationStatus;
import com.example.qubaatisystem.Enum.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationInDTO {

    @NotBlank(message = "title is required")
    @Size(max = 150, message = "title must be at most 150 characters")
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 2000, message = "message must be at most 2000 characters")
    private String message;

    @NotNull(message = "type is required")
    private NotificationType type;

    @NotNull(message = "status is required")
    private NotificationStatus status;

    private java.time.LocalDateTime createdAt;

    private java.time.LocalDateTime readAt;

    @NotNull(message = "recipientId is required")
    private Integer recipientId;
}
