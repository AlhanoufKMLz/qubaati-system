package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.ActivityReviewInDTO;
import com.example.qubaatisystem.Service.ActivityReviewService;
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
@RequestMapping("/api/v1/activity-reviews")
@RequiredArgsConstructor
public class ActivityReviewController {

    private final ActivityReviewService activityReviewService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ActivityReviewInDTO dto) {
        activityReviewService.create(dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityReview created successfully"));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(200).body(activityReviewService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(activityReviewService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody ActivityReviewInDTO dto) {
        activityReviewService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityReview updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        activityReviewService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("ActivityReview deleted successfully"));
    }
}
