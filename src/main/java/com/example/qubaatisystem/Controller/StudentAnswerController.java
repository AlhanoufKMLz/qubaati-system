package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.BatchStudentAnswerInDTO;
import com.example.qubaatisystem.DTO.In.StudentAnswerInDTO;
import com.example.qubaatisystem.DTO.Out.StudentAnswerOutDTO;
import com.example.qubaatisystem.Service.StudentAnswerService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StudentAnswerController {

    private final StudentAnswerService studentAnswerService;

    // ---------- CRUD ----------

    @PostMapping("/student-answers")
    public ResponseEntity<?> create(@Valid @RequestBody StudentAnswerInDTO dto) {
        studentAnswerService.create(dto);
        return ResponseEntity.status(200).body(new ApiResponse("StudentAnswer created successfully"));
    }

    @GetMapping("/student-answers")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(200).body(studentAnswerService.getAll());
    }

    @GetMapping("/student-answers/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(studentAnswerService.getById(id));
    }

    @PutMapping("/student-answers/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody StudentAnswerInDTO dto) {
        studentAnswerService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("StudentAnswer updated successfully"));
    }

    @DeleteMapping("/student-answers/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        studentAnswerService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("StudentAnswer deleted successfully"));
    }

    // ---------- FLOW: BATCH ANSWERS ----------

    @PostMapping("/activity-submissions/{submissionId}/answers/batch")
    public ResponseEntity<List<StudentAnswerOutDTO>> saveBatchAnswers(
            @PathVariable Integer submissionId,
            @Valid @RequestBody BatchStudentAnswerInDTO dto) {
        return ResponseEntity.status(200).body(studentAnswerService.saveBatchAnswers(submissionId, dto));
    }
}
