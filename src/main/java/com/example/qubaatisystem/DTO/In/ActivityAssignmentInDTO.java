package com.example.qubaatisystem.DTO.In;

import com.example.qubaatisystem.Enum.ActivityAssignmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAssignmentInDTO {

    private java.time.LocalDateTime assignedAt;

    private java.time.LocalDateTime dueDate;

    @NotNull(message = "status is required")
    private ActivityAssignmentStatus status;

    @NotNull(message = "activityId is required")
    private Integer activityId;

    private Integer studentId;

    private Integer classroomId;

    // DEPRECATED actor id — IGNORED for the authenticated TEACHER (derived from Basic Auth); only an ADMIN may
    // supply it to act on a teacher's behalf. Optional; do not send it as a teacher.
    private Integer assignedByTeacherId;
}
