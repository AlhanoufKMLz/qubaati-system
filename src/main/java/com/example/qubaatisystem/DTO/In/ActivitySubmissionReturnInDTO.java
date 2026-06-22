package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for return-to-student. The teacher id is required; teacherFeedback is optional (a default message is
 * applied by the service when blank).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySubmissionReturnInDTO {

    // DEPRECATED actor id — IGNORED for the authenticated TEACHER (derived from Basic Auth);
    // only an ADMIN may supply it to act on a teacher behalf. Optional; do not send it as a teacher.
    private Integer teacherId;

    @Size(max = 2000, message = "teacherFeedback must be at most 2000 characters")
    private String teacherFeedback;
}
