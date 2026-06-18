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

    @NotNull(message = "Teacher id is required")
    private Integer teacherId;

    @Size(max = 1000, message = "reviewComment must be at most 1000 characters")
    private String reviewComment;
}
