package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.ActivityAssignmentInDTO;
import com.example.qubaatisystem.Service.ActivityAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/activity-assignments")
@RequiredArgsConstructor
public class ActivityAssignmentController {

    private final ActivityAssignmentService activityAssignmentService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ActivityAssignmentInDTO dto) {
        activityAssignmentService.create(dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityAssignment created successfully"));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(200).body(activityAssignmentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(activityAssignmentService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody ActivityAssignmentInDTO dto) {
        activityAssignmentService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityAssignment updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        activityAssignmentService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityAssignment deleted successfully"));
    }
}
