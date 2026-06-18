package com.example.qubaatisystem;

import com.example.qubaatisystem.Api.ApiException;
import com.example.qubaatisystem.DTO.In.ChildCreateInDTO;
import com.example.qubaatisystem.DTO.In.ChildUpdateProfileInDTO;
import com.example.qubaatisystem.DTO.In.StudentInDTO;
import com.example.qubaatisystem.DTO.Out.StudentOutDTO;
import com.example.qubaatisystem.Model.Parent;
import com.example.qubaatisystem.Repository.ParentRepository;
import com.example.qubaatisystem.Repository.UserRepository;
import com.example.qubaatisystem.Service.ParentService;
import com.example.qubaatisystem.Service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {

    @Mock ParentRepository parentRepository;
    @Mock UserRepository userRepository;
    @Mock StudentService studentService;
    @Mock ModelMapper modelMapper;

    @InjectMocks ParentService parentService;

    // ── createChild ──────────────────────────────────────────────────────────

    @Test
    void createChild_setsParentIdFromUrlPath() {
        Parent parent = new Parent();
        parent.setId(1);
        when(parentRepository.findParentById(1)).thenReturn(parent);

        StudentOutDTO expected = new StudentOutDTO();
        expected.setParentId(1);
        when(studentService.create(any(StudentInDTO.class))).thenReturn(expected);

        ChildCreateInDTO dto = new ChildCreateInDTO(
                "username", "email@test.com", "pass123", "Child Name", 10, "Grade 5", null);

        StudentOutDTO result = parentService.createChild(1, dto);

        assertThat(result.getParentId()).isEqualTo(1);
        // The StudentInDTO passed to create() must carry the parentId from the URL
        verify(studentService).create(argThat(s -> Integer.valueOf(1).equals(s.getParentId())));
    }

    @Test
    void createChild_throwsApiException_whenParentNotFound() {
        when(parentRepository.findParentById(99)).thenReturn(null);

        ChildCreateInDTO dto = new ChildCreateInDTO(
                "u", "e@e.com", "pass123", "Name", 10, "Grade 5", null);

        assertThatThrownBy(() -> parentService.createChild(99, dto))
                .isInstanceOf(ApiException.class);
    }

    // ── getChildren ──────────────────────────────────────────────────────────

    @Test
    void getChildren_throwsApiException_whenParentNotFound() {
        when(parentRepository.findParentById(99)).thenReturn(null);

        assertThatThrownBy(() -> parentService.getChildren(99))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void getChildren_returnsChildListFromStudentService() {
        Parent parent = new Parent();
        parent.setId(1);
        when(parentRepository.findParentById(1)).thenReturn(parent);

        StudentOutDTO child = new StudentOutDTO();
        child.setFullName("My Child");
        when(studentService.getByParentId(1)).thenReturn(List.of(child));

        List<StudentOutDTO> result = parentService.getChildren(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("My Child");
    }

    // ── getChildOverview ─────────────────────────────────────────────────────

    @Test
    void getChildOverview_throwsApiException_whenStudentBelongsToDifferentParent() {
        Parent parent = new Parent();
        parent.setId(1);
        when(parentRepository.findParentById(1)).thenReturn(parent);

        StudentOutDTO studentDTO = new StudentOutDTO();
        studentDTO.setParentId(2); // belongs to a different parent
        when(studentService.getById(5)).thenReturn(studentDTO);

        assertThatThrownBy(() -> parentService.getChildOverview(1, 5))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("does not belong");
    }

    // ── updateChildProfile ───────────────────────────────────────────────────

    @Test
    void updateChildProfile_delegatesOnlyProfileFieldsToStudentService() {
        Parent parent = new Parent();
        parent.setId(1);
        when(parentRepository.findParentById(1)).thenReturn(parent);

        StudentOutDTO existing = new StudentOutDTO();
        existing.setParentId(1);
        when(studentService.getById(3)).thenReturn(existing);

        ChildUpdateProfileInDTO dto = new ChildUpdateProfileInDTO("New Name", 12, "Grade 6");

        StudentOutDTO updated = new StudentOutDTO();
        updated.setFullName("New Name");
        updated.setAge(12);
        when(studentService.updateProfile(3, dto)).thenReturn(updated);

        StudentOutDTO result = parentService.updateChildProfile(1, 3, dto);

        assertThat(result.getFullName()).isEqualTo("New Name");
        verify(studentService).updateProfile(eq(3), eq(dto));
    }
}
