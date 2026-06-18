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

    @NotNull(message = "assignedByTeacherId is required")
    private Integer assignedByTeacherId;

    @NotEmpty(message = "studentIds must not be null or empty")
    private List<Integer> studentIds;

    @Future(message = "dueDate must be in the future")
    private LocalDateTime dueDate;
}
