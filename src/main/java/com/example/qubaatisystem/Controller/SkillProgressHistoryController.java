package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.SkillProgressHistoryInDTO;
import com.example.qubaatisystem.Service.SkillProgressHistoryService;
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
@RequestMapping("/api/v1/skill-progress-history")
@RequiredArgsConstructor
public class SkillProgressHistoryController {

    private final SkillProgressHistoryService skillProgressHistoryService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody SkillProgressHistoryInDTO dto) {
        skillProgressHistoryService.create(dto);
        return ResponseEntity.status(200).body(new ApiResponse("SkillProgressHistory created successfully"));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(200).body(skillProgressHistoryService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(skillProgressHistoryService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody SkillProgressHistoryInDTO dto) {
        skillProgressHistoryService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("SkillProgressHistory updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        skillProgressHistoryService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("SkillProgressHistory deleted successfully"));
    }
}
