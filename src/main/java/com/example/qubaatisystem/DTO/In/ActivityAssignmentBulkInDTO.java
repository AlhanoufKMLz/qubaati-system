package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAssignmentBulkInDTO {

    // DEPRECATED actor id — IGNORED for the authenticated TEACHER (derived from Basic Auth);
    // only an ADMIN may supply it to act on a teacher behalf. Optional; do not send it as a teacher.
    private Integer assignedByTeacherId;

    @NotEmpty(message = "studentIds must not be null or empty")
    private List<Integer> studentIds;

    @Future(message = "dueDate must be in the future")
    private LocalDateTime dueDate;
}
