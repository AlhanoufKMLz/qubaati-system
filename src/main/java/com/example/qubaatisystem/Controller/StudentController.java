package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.StudentInDTO;
import com.example.qubaatisystem.Service.StudentService;
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
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // ---------- CRUD ----------

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody StudentInDTO dto) {
        return ResponseEntity.status(200).body(studentService.create(dto));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(200).body(studentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(studentService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody StudentInDTO dto) {
        return ResponseEntity.status(200).body(studentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        studentService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("Student deleted successfully"));
    }

    // ---------- AVAILABILITY / HISTORY ----------

    @GetMapping("/{studentId}/career-worlds/available")
    public ResponseEntity<?> getAvailableCareerWorlds(@PathVariable Integer studentId) {
        return ResponseEntity.status(200).body(studentService.getAvailableCareerWorlds(studentId));
    }

    @GetMapping("/{studentId}/skills/history")
    public ResponseEntity<?> getSkillHistory(@PathVariable Integer studentId) {
        return ResponseEntity.status(200).body(studentService.getSkillHistory(studentId));
    }

    @GetMapping("/{studentId}/learning-style/history")
    public ResponseEntity<?> getLearningStyleHistory(@PathVariable Integer studentId) {
        return ResponseEntity.status(200).body(studentService.getLearningStyleHistory(studentId));
    }
}
