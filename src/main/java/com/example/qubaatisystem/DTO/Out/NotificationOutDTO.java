package com.example.qubaatisystem.DTO.Out;

import com.example.qubaatisystem.Enum.NotificationStatus;
import com.example.qubaatisystem.Enum.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOutDTO {

    private Integer id;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationStatus status;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime readAt;
    private Integer recipientId;
    private String recipientUsername;
    private String recipientEmail;
}
