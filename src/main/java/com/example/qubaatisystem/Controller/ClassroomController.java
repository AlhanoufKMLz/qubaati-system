package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.ClassroomInDTO;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.ClassroomService;
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
@RequestMapping("/api/v1/classrooms")
@RequiredArgsConstructor
public class ClassroomController {

    private final ClassroomService classroomService;
    private final SecurityOwnershipService security;

    // The classroom owner is derived from Basic Auth: a TEACHER owns it (body teacherId ignored); an ADMIN may
    // supply teacherId to create on a teacher's behalf. The teacher free-plan limit then applies to that teacher.
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ClassroomInDTO dto) {
        dto.setTeacherId(security.resolveOwningTeacherId(dto.getTeacherId()));
        classroomService.create(dto);
        return ResponseEntity.status(200).body(new ApiResponse("Classroom created successfully"));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(200).body(classroomService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(classroomService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody ClassroomInDTO dto) {
        classroomService.update(id, dto);
        return ResponseEntity.status(200).body(new ApiResponse("Classroom updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        classroomService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("Classroom deleted successfully"));
    }

    @PostMapping("/{classroomId}/students/{studentId}/enroll")
    public ResponseEntity<?> enrollStudent(@PathVariable Integer classroomId,
                                           @PathVariable Integer studentId) {
        classroomService.enrollStudent(classroomId, studentId);
        return ResponseEntity.status(200).body(new ApiResponse("Student enrolled in classroom successfully"));
    }

    @DeleteMapping("/{classroomId}/students/{studentId}/remove")
    public ResponseEntity<?> removeStudent(@PathVariable Integer classroomId,
                                           @PathVariable Integer studentId) {
        classroomService.removeStudent(classroomId, studentId);
        return ResponseEntity.status(200).body(new ApiResponse("Student removed from classroom successfully"));
    }

    @GetMapping("/{classroomId}/students")
    public ResponseEntity<?> getStudents(@PathVariable Integer classroomId) {
        return ResponseEntity.status(200).body(classroomService.getStudents(classroomId));
    }

    @GetMapping("/{classroomId}/dashboard")
    public ResponseEntity<?> getDashboard(@PathVariable Integer classroomId) {
        return ResponseEntity.status(200).body(classroomService.getDashboard(classroomId));
    }

    @GetMapping("/{classroomId}/progress")
    public ResponseEntity<?> getProgress(@PathVariable Integer classroomId) {
        return ResponseEntity.status(200).body(classroomService.getProgress(classroomId));
    }
}
