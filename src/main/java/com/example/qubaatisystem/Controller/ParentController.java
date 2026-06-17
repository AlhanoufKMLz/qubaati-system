package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.ChildCreateInDTO;
import com.example.qubaatisystem.DTO.In.ParentInDTO;
import com.example.qubaatisystem.Service.ParentService;
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
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ParentInDTO dto) {
        return ResponseEntity.status(200).body(parentService.create(dto));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.status(200).body(parentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(parentService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody ParentInDTO dto) {
        return ResponseEntity.status(200).body(parentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        parentService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("Parent deleted successfully"));
    }

    @PostMapping("/{parentId}/children")
    public ResponseEntity<?> createChild(@PathVariable Integer parentId,
                                         @Valid @RequestBody ChildCreateInDTO dto) {
        return ResponseEntity.status(200).body(parentService.createChild(parentId, dto));
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<?> getChildren(@PathVariable Integer parentId) {
        return ResponseEntity.status(200).body(parentService.getChildren(parentId));
    }

    @GetMapping("/{parentId}/children/{studentId}/overview")
    public ResponseEntity<?> getChildOverview(@PathVariable Integer parentId,
                                              @PathVariable Integer studentId) {
        return ResponseEntity.status(200).body(parentService.getChildOverview(parentId, studentId));
    }
}
