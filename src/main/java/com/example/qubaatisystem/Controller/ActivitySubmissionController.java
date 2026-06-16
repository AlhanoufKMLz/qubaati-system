package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.ActivitySubmissionInDTO;
import com.example.qubaatisystem.Service.ActivitySubmissionService;
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
@RequestMapping("/api/v1/activity-submissions")
@RequiredArgsConstructor
public class ActivitySubmissionController {

    private final ActivitySubmissionService activitySubmissionService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ActivitySubmissionInDTO dto) {
        activitySubmissionService.create(dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivitySubmission created successfully"));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(200).body(activitySubmissionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(activitySubmissionService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody ActivitySubmissionInDTO dto) {
        activitySubmissionService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivitySubmission updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        activitySubmissionService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("ActivitySubmission deleted successfully"));
    }
}
