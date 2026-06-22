package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Minimal request body for assigning ONE activity (id from path) to ONE student/classroom (id from path).
 * Created because {@code ActivityAssignmentInDTO} has unrelated required fields (status, activityId)
 * that conflict with the path-variable-driven assign endpoints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAssignInDTO {

    // DEPRECATED actor id — IGNORED for the authenticated TEACHER (derived from Basic Auth);
    // only an ADMIN may supply it to act on a teacher behalf. Optional; do not send it as a teacher.
    private Integer assignedByTeacherId;

    // Optional; if provided it must be in the future (validated in ActivityAssignmentService).
    private LocalDateTime dueDate;
}
