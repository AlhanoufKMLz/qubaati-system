package com.example.qubaatisystem.Controller;

import com.example.qubaatisystem.Api.ApiResponse;
import com.example.qubaatisystem.DTO.In.ChildCreateInDTO;
import com.example.qubaatisystem.DTO.In.ChildUpdateProfileInDTO;
import com.example.qubaatisystem.DTO.In.ParentInDTO;
import com.example.qubaatisystem.Security.SecurityOwnershipService;
import com.example.qubaatisystem.Service.ParentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final SecurityOwnershipService security;

    // Creating/listing parents is an admin operation (no public self-registration of parents).
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ParentInDTO dto) {
        security.assertAdmin();
        return ResponseEntity.status(200).body(parentService.create(dto));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        security.assertAdmin();
        return ResponseEntity.status(200).body(parentService.getAll());
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        security.assertCurrentParentOrAdmin(id);
        return ResponseEntity.status(200).body(parentService.getById(id));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody ParentInDTO dto) {
        security.assertCurrentParentOrAdmin(id);
        return ResponseEntity.status(200).body(parentService.update(id, dto));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        security.assertCurrentParentOrAdmin(id);
        parentService.delete(id);
        return ResponseEntity.status(200).body(new ApiResponse("Parent deleted successfully"));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{parentId}/dashboard")
    public ResponseEntity<?> getDashboard(@PathVariable Integer parentId) {
        security.assertCurrentParentOrAdmin(parentId);
        return ResponseEntity.status(200).body(parentService.getDashboard(parentId));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @PostMapping("/{parentId}/children")
    public ResponseEntity<?> createChild(@PathVariable Integer parentId,
                                         @Valid @RequestBody ChildCreateInDTO dto) {
        security.assertCurrentParentOrAdmin(parentId);
        return ResponseEntity.status(200).body(parentService.createChild(parentId, dto));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{parentId}/children")
    public ResponseEntity<?> getChildren(@PathVariable Integer parentId) {
        security.assertCurrentParentOrAdmin(parentId);
        return ResponseEntity.status(200).body(parentService.getChildren(parentId));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{parentId}/children/{studentId}/overview")
    public ResponseEntity<?> getChildOverview(@PathVariable Integer parentId,
                                              @PathVariable Integer studentId) {
        security.assertParentOwnsStudentOrAdmin(parentId, studentId);
        return ResponseEntity.status(200).body(parentService.getChildOverview(parentId, studentId));
    }

    // Combined learning profile: skills, learning style, activity performance, recent mission insight,
    // recommendations, and activity/mission completion for one child.
    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @GetMapping("/{parentId}/children/{studentId}/learning-profile")
    public ResponseEntity<?> getChildLearningProfile(@PathVariable Integer parentId,
                                                     @PathVariable Integer studentId) {
        security.assertParentOwnsStudentOrAdmin(parentId, studentId);
        return ResponseEntity.status(200).body(parentService.getChildLearningProfile(parentId, studentId));
    }

    @Deprecated // legacy profile-id route — prefer the /me equivalent
    @PatchMapping("/{parentId}/children/{studentId}/profile")
    public ResponseEntity<?> updateChildProfile(@PathVariable Integer parentId,
                                                @PathVariable Integer studentId,
                                                @Valid @RequestBody ChildUpdateProfileInDTO dto) {
        security.assertParentOwnsStudentOrAdmin(parentId, studentId);
        return ResponseEntity.status(200).body(parentService.updateChildProfile(parentId, studentId, dto));
    }

    // ---------- current-parent ("me") endpoints — no parentId in the path ----------

    @GetMapping("/me/dashboard")
    public ResponseEntity<?> getMyDashboard() {
        return ResponseEntity.status(200).body(parentService.getDashboard(security.getCurrentParentId()));
    }

    // A parent creates their own child account (the parent is derived from Basic Auth; no parentId in the body).
    @PostMapping("/me/children")
    public ResponseEntity<?> createMyChild(@Valid @RequestBody ChildCreateInDTO dto) {
        return ResponseEntity.status(200).body(parentService.createChild(security.getCurrentParentId(), dto));
    }

    @GetMapping("/me/children")
    public ResponseEntity<?> getMyChildren() {
        return ResponseEntity.status(200).body(parentService.getChildren(security.getCurrentParentId()));
    }

    @GetMapping("/me/children/{studentId}/overview")
    public ResponseEntity<?> getMyChildOverview(@PathVariable Integer studentId) {
        Integer parentId = security.getCurrentParentId();
        security.assertParentOwnsStudentOrAdmin(parentId, studentId);
        return ResponseEntity.status(200).body(parentService.getChildOverview(parentId, studentId));
    }

    @GetMapping("/me/children/{studentId}/learning-profile")
    public ResponseEntity<?> getMyChildLearningProfile(@PathVariable Integer studentId) {
        Integer parentId = security.getCurrentParentId();
        security.assertParentOwnsStudentOrAdmin(parentId, studentId);
        return ResponseEntity.status(200).body(parentService.getChildLearningProfile(parentId, studentId));
    }
}
