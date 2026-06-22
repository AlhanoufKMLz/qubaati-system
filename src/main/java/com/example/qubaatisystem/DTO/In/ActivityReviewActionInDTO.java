package com.example.qubaatisystem.DTO.In;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body for the activity review actions (approve / reject / request-revision). The reviewing teacher id is
 * required; the comment is optional and the service supplies an action-specific default when it is blank.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityReviewActionInDTO {

    // DEPRECATED actor id — IGNORED for the authenticated TEACHER (derived from Basic Auth);
    // only an ADMIN may supply it to act on a teacher behalf. Optional; do not send it as a teacher.
    private Integer teacherId;

    @Size(max = 1000, message = "reviewComment must be at most 1000 characters")
    private String reviewComment;
}
