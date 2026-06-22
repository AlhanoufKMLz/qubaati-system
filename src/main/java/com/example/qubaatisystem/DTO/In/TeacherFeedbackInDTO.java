package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for the add-teacher-feedback endpoint. Both teacher id and feedback are required; this endpoint only
 * attaches feedback and does not change the submission status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherFeedbackInDTO {

    // DEPRECATED actor id — IGNORED for the authenticated TEACHER (derived from Basic Auth);
    // only an ADMIN may supply it to act on a teacher behalf. Optional; do not send it as a teacher.
    private Integer teacherId;

    @NotBlank(message = "Teacher feedback is required")
    @Size(max = 2000, message = "teacherFeedback must be at most 2000 characters")
    private String teacherFeedback;
}
